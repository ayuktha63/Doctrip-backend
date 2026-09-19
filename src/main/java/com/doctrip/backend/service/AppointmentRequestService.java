package com.doctrip.backend.service;

import com.doctrip.backend.dto.AppointmentRequestDetailResponse;
import com.doctrip.backend.dto.AppointmentRequestSummaryResponse;
import com.doctrip.backend.dto.CoordinationLogEntryResponse;
import com.doctrip.backend.dto.DoctorAcknowledgementSummary;
import com.doctrip.backend.dto.DoctorAvailabilityResponse;
import com.doctrip.backend.dto.DoctorCandidateResponse;
import com.doctrip.backend.dto.DoctorResponseRequest;
import com.doctrip.backend.entity.AppointmentRequest;
import com.doctrip.backend.entity.AppointmentRequestStatus;
import com.doctrip.backend.entity.AuditLog;
import com.doctrip.backend.entity.CoordinationDoctor;
import com.doctrip.backend.entity.DoctorAcknowledgementRequest;
import com.doctrip.backend.entity.DoctorAcknowledgementStatus;
import com.doctrip.backend.entity.Hospital;
import com.doctrip.backend.exception.BadRequestException;
import com.doctrip.backend.exception.ConflictException;
import com.doctrip.backend.exception.ForbiddenException;
import com.doctrip.backend.exception.NotFoundException;
import com.doctrip.backend.repository.AppointmentRequestRepository;
import com.doctrip.backend.repository.AuditLogRepository;
import com.doctrip.backend.repository.DoctorAcknowledgementRequestRepository;
import com.doctrip.backend.repository.CoordinationDoctorRepository;
import com.doctrip.backend.repository.HospitalRepository;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-01 — Hospital Admin reviewing appointment requests. FR-02/FR-04/FR-05 — checking doctor
 * availability and initiating acknowledgement. FR-07 — atomically securing the first accepted
 * doctor/slot and closing every other pending request. FR-08 — collecting doctor-proposed
 * alternative slots and letting Hospital Admin select the earliest valid one. FR-10 (partial) —
 * escalating once every contacted doctor has rejected with no alternative on the table.
 *
 * <p>{@code respondToAcknowledgement} simulates the doctor-side action (accept/reject/propose) —
 * the Doctor's own acknowledgement UI/login is a separate story (FR-06's note in the story doc),
 * but the atomic-commit/tie-break/escalation rules it triggers belong here since they mutate this
 * story's AppointmentRequest aggregate (BR-04 — System and Hospital Admin follow the same
 * coordination rules regardless of which story's UI initiated the call).
 *
 * <p>{@link CoordinationDoctorRepository}/{@link CoordinationDoctor} are a minimal placeholder for Doctor master data — a
 * full Doctor-management story (profiles, real availability/calendar) doesn't exist yet (see
 * backend CLAUDE.md non-goals), but coordination inherently needs something to check eligibility
 * against. Replace this service's doctor lookups with the real Doctor data source once that story
 * lands; the {@code doctorId}/{@code doctorName}/{@code doctorSpecialty} snapshot on {@link
 * DoctorAcknowledgementRequest} was chosen specifically so that swap doesn't require a schema
 * change here.
 */
@Service
public class AppointmentRequestService {

    private static final long DOCTOR_RESPONSE_WINDOW_SECONDS = 900; // 15 minutes — placeholder, not spec'd yet.

    private static final List<DoctorAcknowledgementStatus> OPEN_ACK_STATUSES =
            List.of(DoctorAcknowledgementStatus.PENDING, DoctorAcknowledgementStatus.ALTERNATIVE_PROPOSED);

    /** FR-11 — cancellation also terminates an already-ACCEPTED (but not yet Patient-confirmed) ack. */
    private static final List<DoctorAcknowledgementStatus> CANCELLABLE_ACK_STATUSES = List.of(
            DoctorAcknowledgementStatus.PENDING,
            DoctorAcknowledgementStatus.ALTERNATIVE_PROPOSED,
            DoctorAcknowledgementStatus.ACCEPTED);

    /** FR-11 — cancellation is only valid "before confirmation"; these two are both terminal. */
    private static final List<AppointmentRequestStatus> TERMINAL_STATUSES =
            List.of(AppointmentRequestStatus.CANCELLED, AppointmentRequestStatus.CONFIRMED);

    private final AppointmentRequestRepository appointmentRequestRepository;
    private final HospitalRepository hospitalRepository;
    private final CoordinationDoctorRepository doctorRepository;
    private final DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    /** Self-injected (via a lazily-resolved proxy) so calls the class makes to its own {@code
     * REQUIRES_NEW} methods actually go through Spring's transactional proxy — a direct {@code
     * this.method()} call bypasses AOP entirely (the classic Spring self-invocation gap) and would
     * silently run in the caller's existing transaction instead of a new one. */
    private final AppointmentRequestService self;

    public AppointmentRequestService(
            AppointmentRequestRepository appointmentRequestRepository,
            HospitalRepository hospitalRepository,
            CoordinationDoctorRepository doctorRepository,
            DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository,
            AuditLogRepository auditLogRepository,
            AuditService auditService,
            NotificationService notificationService,
            @Lazy AppointmentRequestService self) {
        this.appointmentRequestRepository = appointmentRequestRepository;
        this.hospitalRepository = hospitalRepository;
        this.doctorRepository = doctorRepository;
        this.doctorAcknowledgementRequestRepository = doctorAcknowledgementRequestRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.self = self;
    }

    public List<AppointmentRequestSummaryResponse> listForHospital(UUID hospitalId) {
        return appointmentRequestRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public AppointmentRequestDetailResponse get(UUID id) {
        AppointmentRequest request = findOrThrow(id);
        Hospital hospital = hospitalRepository
                .findById(request.getHospitalId())
                .orElseThrow(() -> new NotFoundException("Hospital not found"));

        auditService.record(request.getPatientId(), "appointment_request_admin_reviewed", null, request.getId());

        return toDetail(request, hospital.getName());
    }

    private static final DateTimeFormatter LOG_TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMM, h:mm a").withZone(ZoneId.systemDefault());

    /**
     * "Coordination log" card (mockup's {@code cur.log}) — a human-readable timeline built from
     * {@link AuditLog} rows this service already writes on every state change, filtered by this
     * request's own id so a patient with several requests never sees another request's history
     * mixed in.
     */
    public List<CoordinationLogEntryResponse> getLog(UUID id) {
        findOrThrow(id);
        return auditLogRepository.findByAppointmentRequestIdOrderByTimestampAsc(id).stream()
                .map(this::toLogEntry)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private CoordinationLogEntryResponse toLogEntry(AuditLog log) {
        String time = LOG_TIME_FORMAT.format(log.getTimestamp());
        String doctor = log.getChannel();
        return switch (log.getEvent()) {
            case "doctor_coordination_started" -> CoordinationLogEntryResponse.builder()
                    .title("Coordination started")
                    .time(time)
                    .body(("SYSTEM".equals(doctor) ? "System took over after the Admin SLA lapsed" : "Hospital Admin started coordination")
                            + " — request sent to every eligible doctor.")
                    .build();
            case "system_sla_takeover" -> CoordinationLogEntryResponse.builder()
                    .title("Coordination taken over")
                    .time(time)
                    .body("No Admin response inside the SLA — the system took over coordination.")
                    .build();
            case "doctor_accepted" -> CoordinationLogEntryResponse.builder()
                    .title("Doctor accepted")
                    .time(time)
                    .body(doctor + " accepted the request and secured the slot.")
                    .build();
            case "doctor_unavailable_at_accept" -> CoordinationLogEntryResponse.builder()
                    .title("Acceptance reversed")
                    .time(time)
                    .body(doctor + " was no longer available — the acceptance could not be applied.")
                    .build();
            case "doctor_rejected" -> CoordinationLogEntryResponse.builder()
                    .title("Doctor declined")
                    .time(time)
                    .body(doctor + " declined the request.")
                    .build();
            case "doctor_alt_proposed" -> CoordinationLogEntryResponse.builder()
                    .title("Alternative proposed")
                    .time(time)
                    .body(doctor + " proposed a different date/time.")
                    .build();
            case "doctor_ack_expired" -> CoordinationLogEntryResponse.builder()
                    .title("No response in time")
                    .time(time)
                    .body(doctor + " did not respond inside the response window.")
                    .build();
            case "alt_slot_selected" -> CoordinationLogEntryResponse.builder()
                    .title("Proposal sent")
                    .time(time)
                    .body("Hospital Admin selected " + doctor + "'s proposed slot and sent it to the patient.")
                    .build();
            case "coordination_escalated" -> CoordinationLogEntryResponse.builder()
                    .title("Escalated")
                    .time(time)
                    .body("NO_DOCTORS".equals(doctor)
                            ? "No eligible doctors were available — escalated back to Hospital Admin."
                            : "Every contacted doctor declined with no valid alternative — escalated back to Hospital Admin.")
                    .build();
            case "patient_confirmed" -> CoordinationLogEntryResponse.builder()
                    .title("Patient confirmed")
                    .time(time)
                    .body("The patient confirmed the proposed appointment.")
                    .build();
            case "patient_cancelled" -> CoordinationLogEntryResponse.builder()
                    .title("Cancelled by patient")
                    .time(time)
                    .body("The patient cancelled this request.")
                    .build();
            default -> null;
        };
    }

    /** FR-02/FR-04/FR-05 — preview who's eligible before actually sending acknowledgement requests. */
    public DoctorAvailabilityResponse getDoctorAvailability(UUID id) {
        AppointmentRequest request = findOrThrow(id);
        List<CoordinationDoctor> eligible =
                doctorRepository.findByHospitalIdAndDepartmentAndAvailableTrue(request.getHospitalId(), request.getDepartment());

        boolean requestedDoctorAvailable = request.getRequestedDoctorName() != null
                && eligible.stream().anyMatch(d -> d.getName().equalsIgnoreCase(request.getRequestedDoctorName()));

        return new DoctorAvailabilityResponse(
                request.getRequestedDoctorName(),
                requestedDoctorAvailable,
                eligible.stream().map(d -> new DoctorCandidateResponse(d.getId(), d.getName(), d.getSpecialty())).toList());
    }

    /**
     * FR-02/FR-04/FR-05 — starts Doctor coordination: sends the requested doctor an acknowledgement
     * request if they're eligible and available (FR-04), otherwise fans out simultaneously to every
     * eligible doctor in the same department (FR-05). BR-05 — availability is (re)validated here,
     * right before sending, not just whenever the admin last looked.
     */
    @Transactional
    public AppointmentRequestDetailResponse startCoordination(UUID id) {
        AppointmentRequest request = findOrThrow(id);

        if (request.getStatus() != AppointmentRequestStatus.REQUESTED) {
            throw new ConflictException("Doctor coordination has already been started for this request.");
        }

        beginCoordination(request, "ADMIN");

        Hospital hospital = hospitalRepository
                .findById(request.getHospitalId())
                .orElseThrow(() -> new NotFoundException("Hospital not found"));
        return toDetail(request, hospital.getName());
    }

    /**
     * FR-02/FR-04/FR-05 fan-out logic, shared by the Hospital-Admin-initiated path ({@link
     * #startCoordination}) and the System-initiated one ({@link AdminSlaEnforcer}, FR-03) — BR-04
     * requires both to follow identical rules. Caller must have already checked {@code request} is
     * still {@code REQUESTED}. Throws {@link ConflictException} if no doctor is eligible; the System
     * path (unlike the Admin path) catches that and escalates instead of surfacing an error to no one.
     */
    private void beginCoordination(AppointmentRequest request, String initiator) {
        List<CoordinationDoctor> eligible =
                doctorRepository.findByHospitalIdAndDepartmentAndAvailableTrue(request.getHospitalId(), request.getDepartment());

        CoordinationDoctor requestedDoctor = request.getRequestedDoctorName() == null
                ? null
                : eligible.stream()
                        .filter(d -> d.getName().equalsIgnoreCase(request.getRequestedDoctorName()))
                        .findFirst()
                        .orElse(null);

        List<CoordinationDoctor> targets = requestedDoctor != null ? List.of(requestedDoctor) : eligible;

        if (targets.isEmpty()) {
            throw new ConflictException(
                    "No eligible doctors are available in " + request.getDepartment() + " right now.");
        }

        Instant now = Instant.now();
        Instant deadline = now.plusSeconds(DOCTOR_RESPONSE_WINDOW_SECONDS);
        for (CoordinationDoctor doctor : targets) {
            doctorAcknowledgementRequestRepository.save(DoctorAcknowledgementRequest.builder()
                    .appointmentRequestId(request.getId())
                    .doctorId(doctor.getId())
                    .doctorName(doctor.getName())
                    .doctorSpecialty(doctor.getSpecialty())
                    .sentAt(now)
                    .responseDeadline(deadline)
                    .build());
            auditService.record(
                    request.getPatientId(),
                    "doctor_acknowledgement_requested",
                    requestedDoctor != null ? "REQUESTED" : "ALTERNATIVE",
                    request.getId());
            notificationService.notify(
                    "DOCTOR",
                    doctor.getId().toString(),
                    "doctor_acknowledgement_request",
                    "New appointment request in " + request.getDepartment() + " for " + request.getRequestedDate()
                            + " at " + request.getRequestedTime() + ".");
        }

        request.setStatus(AppointmentRequestStatus.DOCTOR_COORDINATION);
        appointmentRequestRepository.save(request);
        auditService.record(request.getPatientId(), "doctor_coordination_started", initiator, request.getId());
    }

    /**
     * FR-03 — invoked only by {@link AdminSlaEnforcer} once a request's Admin SLA has passed while
     * still {@code REQUESTED}. BR-02: the System follows the exact same coordination rules as
     * Hospital Admin ({@link #beginCoordination}); the only difference from {@link
     * #startCoordination} is what happens when nobody is eligible — there's no admin waiting on an
     * error response here, so that case escalates directly (FR-10) instead of throwing.
     */
    @Transactional
    public void systemTakeOverIfStillRequested(UUID id) {
        AppointmentRequest request = findOrThrow(id);
        if (request.getStatus() != AppointmentRequestStatus.REQUESTED) {
            return; // Hospital Admin (or an earlier scheduler tick) already acted — BR-03/EC-13.
        }
        if (Instant.now().isBefore(request.getAdminSlaDeadline())) {
            return; // Defensive — the caller's query should already filter this, but don't trust it twice-removed.
        }

        auditService.record(request.getPatientId(), "system_sla_takeover", null, request.getId());
        notificationService.notify(
                "ADMIN", request.getHospitalId().toString(), "system_sla_takeover",
                "Admin SLA breached for " + request.getRequestCode() + " — the System has taken over coordination.");
        try {
            beginCoordination(request, "SYSTEM");
        } catch (ConflictException noEligibleDoctors) {
            if (appointmentRequestRepository.compareAndSetStatus(
                            request.getId(),
                            AppointmentRequestStatus.REQUESTED,
                            AppointmentRequestStatus.ESCALATED,
                            Instant.now())
                    == 1) {
                auditService.record(request.getPatientId(), "coordination_escalated", "NO_DOCTORS", request.getId());
                notifyEscalation(request);
            }
        }
    }

    /**
     * FR-11 — Patient cancellation, available at any point before Patient Confirmation (which
     * doesn't exist yet in this slice — see BR-20, nothing here is ever "Confirmed" yet). Stops
     * coordination, closes every acknowledgement request (including an already-ACCEPTED one — AC-15),
     * and releases the Booking Buffer. Only available before Patient Confirmation — {@link #confirm}
     * is the terminal state that closes this window off.
     *
     * <p>FR-12/Booking Buffer — this backend doesn't model the Buffer as its own row: {@code
     * AppointmentRequestStatus} already IS the hold/no-hold state machine (every status except
     * CANCELLED/CONFIRMED represents an active hold), so "release the Buffer" is simply this cancel
     * transition. If a future FR needs the Buffer to outlive the request's own lifecycle (e.g. a
     * timed hold independent of status), promote it to a real column/entity then — don't add one
     * speculatively now.
     */
    @Transactional
    public AppointmentRequestDetailResponse cancel(UUID id, String patientId) {
        AppointmentRequest request = findOrThrow(id);
        if (!request.getPatientId().equals(patientId)) {
            throw new ForbiddenException("This appointment request does not belong to you.");
        }

        // AC-15/AC-16/BR-18 — same atomic compare-and-set shape as doctor acceptance: whichever
        // update's row lock commits first wins, so a doctor accepting concurrently either loses this
        // race (their compareAndSetStatus(DOCTOR_COORDINATION -> DOCTOR_SELECTED) then sees the row is
        // already CANCELLED and fails) or already won before this call arrived (in which case
        // cancellation still succeeds — FR-11 explicitly allows cancelling an unconfirmed
        // DOCTOR_SELECTED appointment, since nothing is Confirmed yet). Once CONFIRMED, though,
        // cancellation is no longer "before confirmation" and this update correctly matches 0 rows.
        Instant now = Instant.now();
        if (appointmentRequestRepository.cancel(id, AppointmentRequestStatus.CANCELLED, TERMINAL_STATUSES, now) == 0) {
            throw new ConflictException(request.getStatus() == AppointmentRequestStatus.CONFIRMED
                    ? "This appointment has already been confirmed and can no longer be cancelled here."
                    : "This appointment request has already been cancelled.");
        }

        // FR-09/Notifications table — "Patient cancellation" notifies Hospital Admin and any
        // Active Doctor(s), so gather who was still active *before* closing their acks below.
        List<DoctorAcknowledgementRequest> activeAcks = doctorAcknowledgementRequestRepository
                .findByAppointmentRequestIdOrderBySentAtAsc(id).stream()
                .filter(a -> CANCELLABLE_ACK_STATUSES.contains(a.getStatus()))
                .toList();

        doctorAcknowledgementRequestRepository.closeAllOpen(
                id, CANCELLABLE_ACK_STATUSES, DoctorAcknowledgementStatus.CLOSED, now);
        auditService.record(patientId, "patient_cancelled", null, id);

        notificationService.notify(
                "ADMIN", request.getHospitalId().toString(), "patient_cancelled",
                "Patient cancelled request " + request.getRequestCode() + ".");
        for (DoctorAcknowledgementRequest a : activeAcks) {
            notificationService.notify(
                    "DOCTOR", a.getDoctorId().toString(), "patient_cancelled",
                    "The patient cancelled the appointment request you were coordinating.");
        }

        AppointmentRequest refreshed = findOrThrow(id);
        Hospital hospital = hospitalRepository
                .findById(refreshed.getHospitalId())
                .orElseThrow(() -> new NotFoundException("Hospital not found"));
        return toDetail(refreshed, hospital.getName());
    }

    /**
     * Patient Confirmation — the step that turns a Hospital-Admin/System-secured
     * {@code DOCTOR_SELECTED} doctor+slot into an actually {@code CONFIRMED} appointment (BR-16,
     * BR-20, BR-21, AC-18: nothing before this point is ever "Confirmed", no matter how far
     * coordination has gotten). Same ownership check and atomic compare-and-set pattern as {@link
     * #cancel}; only valid from {@code DOCTOR_SELECTED} — there's nothing to confirm from any other
     * status.
     */
    @Transactional
    public AppointmentRequestDetailResponse confirm(UUID id, String patientId) {
        AppointmentRequest request = findOrThrow(id);
        if (!request.getPatientId().equals(patientId)) {
            throw new ForbiddenException("This appointment request does not belong to you.");
        }

        if (appointmentRequestRepository.compareAndSetStatus(
                        id, AppointmentRequestStatus.DOCTOR_SELECTED, AppointmentRequestStatus.CONFIRMED, Instant.now())
                == 0) {
            throw new ConflictException(
                    switch (request.getStatus()) {
                        case CONFIRMED -> "This appointment has already been confirmed.";
                        case CANCELLED -> "This appointment request was cancelled and can no longer be confirmed.";
                        default -> "There is nothing to confirm yet for this appointment request.";
                    });
        }

        auditService.record(patientId, "patient_confirmed", null, id);

        // FR-09/Notifications table — "Patient accepts alternative" notifies Doctor + Hospital
        // Admin; applied here for any confirmation, not just an alternative-slot one, since both
        // paths reach DOCTOR_SELECTED -> CONFIRMED the same way and both deserve the same notice.
        doctorAcknowledgementRequestRepository.findByAppointmentRequestIdAndStatus(id, DoctorAcknowledgementStatus.ACCEPTED)
                .forEach(a -> notificationService.notify(
                        "DOCTOR", a.getDoctorId().toString(), "patient_confirmed",
                        "The patient confirmed appointment " + request.getRequestCode() + "."));
        notificationService.notify(
                "ADMIN", request.getHospitalId().toString(), "patient_confirmed",
                "Patient confirmed request " + request.getRequestCode() + ".");

        AppointmentRequest refreshed = findOrThrow(id);
        Hospital hospital = hospitalRepository
                .findById(refreshed.getHospitalId())
                .orElseThrow(() -> new NotFoundException("Hospital not found"));
        return toDetail(refreshed, hospital.getName());
    }

    /**
     * FR-06/FR-07/FR-08 — a doctor's response to one acknowledgement request. See this class's doc
     * comment for why the response-handling logic (not just the send) lives in this service.
     */
    @Transactional
    public AppointmentRequestDetailResponse respondToAcknowledgement(UUID ackId, DoctorResponseRequest body) {
        DoctorAcknowledgementRequest ack = doctorAcknowledgementRequestRepository
                .findById(ackId)
                .orElseThrow(() -> new NotFoundException("Acknowledgement request not found"));
        AppointmentRequest request = findOrThrow(ack.getAppointmentRequestId());

        String action = body.action() == null ? "" : body.action().trim().toUpperCase();
        switch (action) {
            case "ACCEPT" -> acceptAcknowledgement(request, ack);
            case "REJECT" -> rejectAcknowledgement(request, ack);
            case "PROPOSE_ALTERNATIVE" -> proposeAlternative(request, ack, body);
            default -> throw new BadRequestException("action must be ACCEPT, REJECT, or PROPOSE_ALTERNATIVE.");
        }

        AppointmentRequest refreshed = findOrThrow(request.getId());
        Hospital hospital = hospitalRepository
                .findById(refreshed.getHospitalId())
                .orElseThrow(() -> new NotFoundException("Hospital not found"));
        return toDetail(refreshed, hospital.getName());
    }

    private void acceptAcknowledgement(AppointmentRequest request, DoctorAcknowledgementRequest ack) {
        // EC-01/EC-02 — revalidate the doctor is still available right before committing; a doctor
        // whose availability flipped after the ack was sent must not secure the slot. Treated as a
        // rejection (their round ends, others continue), not a hard error, since this reflects an
        // availability change rather than the caller doing anything wrong.
        CoordinationDoctor doctor = doctorRepository.findById(ack.getDoctorId()).orElse(null);
        if (doctor == null || !doctor.isAvailable()) {
            // REQUIRES_NEW — this rejection (and any resulting escalation) must survive independently
            // of the ConflictException we're about to throw, which would otherwise roll back the
            // current transaction and silently undo it (this bit the outer flow's every other
            // "mutate, then throw" case avoids only because AuditService's own writes are already
            // REQUIRES_NEW — this is the first place a *state* mutation, not just an audit row,
            // needed the same treatment).
            self.rejectDueToUnavailability(ack.getId());
            throw new ConflictException("This doctor is no longer available; the acceptance could not be applied.");
        }

        Instant now = Instant.now();
        // AC-14/EC-05 — a response to an ack that's no longer open (already closed/accepted/rejected,
        // or the coordination window moved on) never modifies the appointment.
        if (doctorAcknowledgementRequestRepository.compareAndSetStatus(
                        ack.getId(), DoctorAcknowledgementStatus.PENDING, DoctorAcknowledgementStatus.ACCEPTED, now)
                == 0) {
            throw new ConflictException("This acknowledgement request is no longer active.");
        }

        // FR-07/BR-09/BR-11 — atomic: only the first caller to see DOCTOR_COORDINATION wins the flip
        // to DOCTOR_SELECTED. See AppointmentRequestRepository#compareAndSetStatus's doc comment for
        // how Postgres row locking makes this safe under concurrent/simultaneous acceptance
        // (AC-09/AC-10/EC-04) without an application-level lock.
        int won = appointmentRequestRepository.compareAndSetStatus(
                request.getId(), AppointmentRequestStatus.DOCTOR_COORDINATION, AppointmentRequestStatus.DOCTOR_SELECTED, now);
        if (won == 0) {
            // Someone else's acceptance/alternative-resolution already secured this appointment first,
            // OR the Patient cancelled it in the meantime (AC-16/BR-18) — either way this ack's own
            // ACCEPTED flip above must be undone; it lost the race. REQUIRES_NEW for the same reason
            // as rejectDueToUnavailability — this undo must survive the ConflictException below.
            self.closeLosingAcceptance(ack.getId());
            throw new ConflictException("This appointment request is no longer open for doctor coordination.");
        }

        doctorAcknowledgementRequestRepository.closeOthers(
                request.getId(), ack.getId(), OPEN_ACK_STATUSES, DoctorAcknowledgementStatus.CLOSED, now);
        auditService.record(request.getPatientId(), "doctor_accepted", ack.getDoctorName(), request.getId());
        notificationService.notify(
                "PATIENT",
                request.getPatientId(),
                "doctor_accepted",
                ack.getDoctorName() + " accepted your appointment request. It's ready for your confirmation.");
    }

    /**
     * EC-01/EC-02 — runs in its own transaction (see the call site in {@link #acceptAcknowledgement}
     * for why) so this rejection and any resulting escalation commit regardless of the
     * ConflictException the caller throws right after invoking this.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectDueToUnavailability(UUID ackId) {
        DoctorAcknowledgementRequest ack = doctorAcknowledgementRequestRepository.findById(ackId).orElse(null);
        if (ack == null) return;
        if (doctorAcknowledgementRequestRepository.compareAndSetStatus(
                        ackId, DoctorAcknowledgementStatus.PENDING, DoctorAcknowledgementStatus.REJECTED, Instant.now())
                == 0) {
            return; // already resolved (accepted/rejected/expired/closed) by someone else in the meantime.
        }
        AppointmentRequest request = appointmentRequestRepository.findById(ack.getAppointmentRequestId()).orElse(null);
        if (request == null) return;
        auditService.record(request.getPatientId(), "doctor_unavailable_at_accept", ack.getDoctorName(), request.getId());
        escalateIfExhausted(request);
    }

    /**
     * FR-07/AC-09/AC-14 — undoes a losing acceptance (this ack had flipped to ACCEPTED, but lost
     * the race to secure the request). REQUIRES_NEW for the same reason as {@link
     * #rejectDueToUnavailability} — see the call site in {@link #acceptAcknowledgement}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeLosingAcceptance(UUID ackId) {
        doctorAcknowledgementRequestRepository.compareAndSetStatus(
                ackId, DoctorAcknowledgementStatus.ACCEPTED, DoctorAcknowledgementStatus.CLOSED, Instant.now());
    }

    private void rejectAcknowledgement(AppointmentRequest request, DoctorAcknowledgementRequest ack) {
        if (doctorAcknowledgementRequestRepository.compareAndSetStatus(
                        ack.getId(), DoctorAcknowledgementStatus.PENDING, DoctorAcknowledgementStatus.REJECTED, Instant.now())
                == 0) {
            throw new ConflictException("This acknowledgement request is no longer active.");
        }
        auditService.record(request.getPatientId(), "doctor_rejected", ack.getDoctorName(), request.getId());
        escalateIfExhausted(request);
    }

    private void proposeAlternative(AppointmentRequest request, DoctorAcknowledgementRequest ack, DoctorResponseRequest body) {
        if (body.proposedDate() == null || body.proposedTime() == null) {
            throw new BadRequestException("proposedDate and proposedTime are required for PROPOSE_ALTERNATIVE.");
        }
        if (body.proposedDate().isBefore(java.time.LocalDate.now())) {
            throw new BadRequestException("proposedDate must not be in the past.");
        }

        if (doctorAcknowledgementRequestRepository.proposeAlternative(
                        ack.getId(),
                        DoctorAcknowledgementStatus.PENDING,
                        DoctorAcknowledgementStatus.ALTERNATIVE_PROPOSED,
                        body.proposedDate(),
                        body.proposedTime(),
                        Instant.now())
                == 0) {
            throw new ConflictException("This acknowledgement request is no longer active.");
        }
        // BR-21 — the Patient is notified once a valid alternative is actually selected
        // (resolveAlternative), not on every individual raw proposal from each doctor.
        auditService.record(request.getPatientId(), "doctor_alt_proposed", ack.getDoctorName(), request.getId());
    }

    /**
     * EC-12 — invoked by {@link DoctorResponseTimeoutEnforcer}. A doctor who never responds at all
     * (still PENDING once their {@code responseDeadline} passes) has their ack closed, ending that
     * round for them; if that leaves the request with nothing else open (FR-10/EC-10), it escalates
     * via the exact same {@link #escalateIfExhausted} path a rejection would. Doesn't touch
     * ALTERNATIVE_PROPOSED acks — proposing a slot IS a response, so EC-12 doesn't apply to it; that
     * one waits on Hospital Admin's {@link #resolveAlternative}, not a timer.
     */
    @Transactional
    public void expireOverdueAcknowledgements() {
        List<DoctorAcknowledgementRequest> overdue = doctorAcknowledgementRequestRepository
                .findByStatusAndResponseDeadlineBefore(DoctorAcknowledgementStatus.PENDING, Instant.now());

        for (UUID requestId : overdue.stream().map(DoctorAcknowledgementRequest::getAppointmentRequestId).distinct().toList()) {
            AppointmentRequest request = appointmentRequestRepository.findById(requestId).orElse(null);
            // Guard against a request that moved on (secured/cancelled/already escalated) between
            // the query above and now — same stale-read tolerance as everywhere else in this class.
            if (request == null || request.getStatus() != AppointmentRequestStatus.DOCTOR_COORDINATION) continue;

            Instant now = Instant.now();
            for (DoctorAcknowledgementRequest ack : overdue) {
                if (!ack.getAppointmentRequestId().equals(requestId)) continue;
                if (doctorAcknowledgementRequestRepository.compareAndSetStatus(
                                ack.getId(), DoctorAcknowledgementStatus.PENDING, DoctorAcknowledgementStatus.CLOSED, now)
                        == 1) {
                    auditService.record(request.getPatientId(), "doctor_ack_expired", ack.getDoctorName(), request.getId());
                }
            }
            escalateIfExhausted(request);
        }
    }

    /** FR-10/EC-10 — no doctor accepted and none proposed an alternative still on the table. */
    private void escalateIfExhausted(AppointmentRequest request) {
        long stillOpen = doctorAcknowledgementRequestRepository.countByAppointmentRequestIdAndStatusIn(
                request.getId(), OPEN_ACK_STATUSES);
        if (stillOpen == 0
                && appointmentRequestRepository.compareAndSetStatus(
                                request.getId(),
                                AppointmentRequestStatus.DOCTOR_COORDINATION,
                                AppointmentRequestStatus.ESCALATED,
                                Instant.now())
                        == 1) {
            auditService.record(request.getPatientId(), "coordination_escalated", null, request.getId());
            notifyEscalation(request);
        }
    }

    /** FR-09/FR-10 — "Doctor coordination cannot be completed" notifies both the Patient and
     * Hospital Admin (the story's Notifications table lists both as recipients for this event). */
    private void notifyEscalation(AppointmentRequest request) {
        notificationService.notify(
                "PATIENT",
                request.getPatientId(),
                "coordination_failed",
                "We couldn't confirm a doctor for your appointment request. Our team will follow up.");
        notificationService.notify(
                "ADMIN",
                request.getHospitalId().toString(),
                "coordination_failed",
                "Request " + request.getRequestCode() + " needs manual follow-up — no doctor accepted and no valid alternative was proposed.");
    }

    /**
     * FR-08/BR-15/BR-16 — Hospital Admin selects the earliest valid doctor-proposed alternative
     * (BR-10 tie-break: doctor id ordering for same-time proposals) and secures it the same atomic
     * way as a direct acceptance (FR-07). The slot stays a proposal until Patient Confirmation
     * (BR-16) — not built in this slice.
     */
    @Transactional
    public AppointmentRequestDetailResponse resolveAlternative(UUID requestId) {
        AppointmentRequest request = findOrThrow(requestId);
        if (request.getStatus() != AppointmentRequestStatus.DOCTOR_COORDINATION) {
            throw new ConflictException("This request is not awaiting an alternative-slot decision.");
        }

        List<DoctorAcknowledgementRequest> proposals = doctorAcknowledgementRequestRepository
                .findByAppointmentRequestIdAndStatus(requestId, DoctorAcknowledgementStatus.ALTERNATIVE_PROPOSED);
        if (proposals.isEmpty()) {
            throw new ConflictException("No alternative proposals are available to select yet.");
        }

        // EC-09 — revalidate each proposing doctor is still available before picking one; a
        // proposal from a doctor who's since become unavailable is discarded, and the next-earliest
        // remaining valid one is used instead (walking down the sorted list below naturally does
        // this once the stale ones are filtered out).
        List<DoctorAcknowledgementRequest> stillValid = proposals.stream()
                .filter(p -> doctorRepository.findById(p.getDoctorId()).map(CoordinationDoctor::isAvailable).orElse(false))
                .toList();
        if (stillValid.isEmpty()) {
            throw new ConflictException("None of the proposed alternatives are still valid — every proposing doctor is now unavailable.");
        }

        DoctorAcknowledgementRequest earliest = stillValid.stream()
                .min(Comparator
                        .comparing(DoctorAcknowledgementRequest::getProposedDate)
                        .thenComparing(DoctorAcknowledgementRequest::getProposedTime)
                        .thenComparing(DoctorAcknowledgementRequest::getDoctorId))
                .orElseThrow();

        Instant now = Instant.now();
        int won = appointmentRequestRepository.compareAndSetStatus(
                requestId, AppointmentRequestStatus.DOCTOR_COORDINATION, AppointmentRequestStatus.DOCTOR_SELECTED, now);
        if (won == 0) {
            throw new ConflictException("This appointment has already been secured.");
        }
        doctorAcknowledgementRequestRepository.compareAndSetStatus(
                earliest.getId(), DoctorAcknowledgementStatus.ALTERNATIVE_PROPOSED, DoctorAcknowledgementStatus.ACCEPTED, now);
        doctorAcknowledgementRequestRepository.closeOthers(
                requestId, earliest.getId(), OPEN_ACK_STATUSES, DoctorAcknowledgementStatus.CLOSED, now);

        auditService.record(request.getPatientId(), "alt_slot_selected", earliest.getDoctorName(), request.getId());
        notificationService.notify(
                "PATIENT",
                request.getPatientId(),
                "alternative_available",
                earliest.getDoctorName() + " can see you on " + earliest.getProposedDate() + " at "
                        + earliest.getProposedTime() + ". Please confirm to finalize.");

        AppointmentRequest refreshed = findOrThrow(requestId);
        Hospital hospital = hospitalRepository
                .findById(refreshed.getHospitalId())
                .orElseThrow(() -> new NotFoundException("Hospital not found"));
        return toDetail(refreshed, hospital.getName());
    }

    private AppointmentRequest findOrThrow(UUID id) {
        return appointmentRequestRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment request not found"));
    }

    private AppointmentRequestSummaryResponse toSummary(AppointmentRequest r) {
        boolean breached = Instant.now().isAfter(r.getAdminSlaDeadline());
        return new AppointmentRequestSummaryResponse(
                r.getId(),
                r.getRequestCode(),
                r.getPatientName(),
                r.getPatientCountry(),
                r.getDepartment(),
                r.getRequestedDoctorName(),
                r.getRequestedDate(),
                r.getRequestedTime(),
                r.getStatus(),
                r.getAdminSlaDeadline(),
                breached);
    }

    private AppointmentRequestDetailResponse toDetail(AppointmentRequest r, String hospitalName) {
        boolean breached = Instant.now().isAfter(r.getAdminSlaDeadline());
        List<DoctorAcknowledgementRequest> acks =
                doctorAcknowledgementRequestRepository.findByAppointmentRequestIdOrderBySentAtAsc(r.getId());

        List<DoctorAcknowledgementSummary> coordination = acks.stream()
                .map(a -> new DoctorAcknowledgementSummary(
                        a.getId(),
                        a.getDoctorId(),
                        a.getDoctorName(),
                        a.getDoctorSpecialty(),
                        a.getStatus(),
                        a.getSentAt(),
                        a.getResponseDeadline(),
                        a.getProposedDate(),
                        a.getProposedTime()))
                .toList();

        DoctorAcknowledgementRequest secured = acks.stream()
                .filter(a -> a.getStatus() == DoctorAcknowledgementStatus.ACCEPTED)
                .findFirst()
                .orElse(null);

        return new AppointmentRequestDetailResponse(
                r.getId(),
                r.getRequestCode(),
                r.getPatientId(),
                r.getPatientName(),
                r.getPatientCountry(),
                r.getPatientAge(),
                r.getPatientGender(),
                r.getHospitalId(),
                hospitalName,
                r.getDepartment(),
                r.getRequestedDoctorName(),
                r.getRequestedDate(),
                r.getRequestedTime(),
                r.getHealthcareRequirement(),
                r.getAdditionalInfo(),
                r.getRequiredDocuments(),
                r.getStatus(),
                r.getAdminSlaDeadline(),
                breached,
                r.getCreatedAt(),
                r.getUpdatedAt(),
                coordination,
                secured == null ? null : secured.getDoctorName(),
                secured == null ? null : (secured.getProposedDate() != null ? secured.getProposedDate() : r.getRequestedDate()),
                secured == null ? null : (secured.getProposedTime() != null ? secured.getProposedTime() : r.getRequestedTime()));
    }
}
