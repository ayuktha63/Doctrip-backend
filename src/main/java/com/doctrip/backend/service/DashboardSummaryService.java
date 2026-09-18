package com.doctrip.backend.service;

import com.doctrip.backend.dto.hospitaladmin.DashboardSummaryResponse;
import com.doctrip.backend.dto.hospitaladmin.DashboardSummaryResponse.Kpis;
import com.doctrip.backend.dto.hospitaladmin.DashboardSummaryResponse.NeedsAttentionItem;
import com.doctrip.backend.dto.hospitaladmin.DashboardSummaryResponse.Pipeline;
import com.doctrip.backend.dto.hospitaladmin.DashboardSummaryResponse.SlaAlertItem;
import com.doctrip.backend.dto.hospitaladmin.DashboardSummaryResponse.UpcomingAppointmentItem;
import com.doctrip.backend.entity.AppointmentRequest;
import com.doctrip.backend.entity.AppointmentRequestStatus;
import com.doctrip.backend.entity.DoctorAcknowledgementRequest;
import com.doctrip.backend.entity.DoctorAcknowledgementStatus;
import com.doctrip.backend.repository.AppointmentRequestRepository;
import com.doctrip.backend.repository.DoctorAcknowledgementRequestRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * Read-only aggregation over the Hospital Appointment Coordination story's real
 * {@link AppointmentRequest}/{@link DoctorAcknowledgementRequest} entities (Story DT-6) for the
 * Hospital Admin Dashboard's "Coordination desk" summary. No state-transition logic lives here —
 * that's entirely AppointmentRequestService's (DT-6) job; this only reads and buckets.
 *
 * <p>DT-6's status model has no exact 1:1 mapping to this dashboard's 6-bucket contract
 * (new/inCoordination/awaitingDoctor/awaitingPatient/confirmed/cancelled), since it doesn't
 * distinguish "in coordination" from "awaiting a specific doctor's response" as separate states —
 * both are {@link AppointmentRequestStatus#DOCTOR_COORDINATION}. That single status is counted
 * into both buckets. {@link AppointmentRequestStatus#ESCALATED} (no eligible doctor found) is
 * folded into "awaitingDoctor" too — it's still an open request needing Hospital Admin's
 * attention, just not any doctor's.
 */
@Service
public class DashboardSummaryService {

    private static final Set<AppointmentRequestStatus> OPEN_STATUSES = Set.of(
            AppointmentRequestStatus.REQUESTED,
            AppointmentRequestStatus.DOCTOR_COORDINATION,
            AppointmentRequestStatus.DOCTOR_SELECTED,
            AppointmentRequestStatus.ESCALATED);

    private static final int NEEDS_ATTENTION_LIMIT = 8;
    private static final int SLA_ALERTS_LIMIT = 8;
    private static final int UPCOMING_LIMIT = 5;

    private final AppointmentRequestRepository appointmentRequestRepository;
    private final DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository;

    public DashboardSummaryService(
            AppointmentRequestRepository appointmentRequestRepository,
            DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository) {
        this.appointmentRequestRepository = appointmentRequestRepository;
        this.doctorAcknowledgementRequestRepository = doctorAcknowledgementRequestRepository;
    }

    public DashboardSummaryResponse summarize(UUID hospitalId) {
        List<AppointmentRequest> all = appointmentRequestRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId);
        Instant now = Instant.now();

        long newRequests = countByStatus(all, AppointmentRequestStatus.REQUESTED);
        long inCoordination = countByStatus(all, AppointmentRequestStatus.DOCTOR_COORDINATION);
        long awaitingDoctor = inCoordination + countByStatus(all, AppointmentRequestStatus.ESCALATED);
        long awaitingPatient = countByStatus(all, AppointmentRequestStatus.DOCTOR_SELECTED);
        long confirmed = countByStatus(all, AppointmentRequestStatus.CONFIRMED);
        long cancelled = countByStatus(all, AppointmentRequestStatus.CANCELLED);

        List<AppointmentRequest> open = all.stream()
                .filter(r -> OPEN_STATUSES.contains(r.getStatus()))
                .sorted(Comparator.comparingLong(r -> slaMinutesRemaining(r, now)))
                .toList();

        long slaBreaches = open.stream().filter(r -> slaMinutesRemaining(r, now) < 0).count();
        List<AppointmentRequest> confirmedUpcoming = all.stream()
                .filter(r -> AppointmentRequestStatus.CONFIRMED.equals(r.getStatus()))
                .filter(r -> effectiveScheduledAt(r).isAfter(now))
                .sorted(Comparator.comparing(this::effectiveScheduledAt))
                .toList();

        Kpis kpis = new Kpis(newRequests, inCoordination, awaitingDoctor, awaitingPatient, confirmedUpcoming.size(), slaBreaches);
        Pipeline pipeline = new Pipeline(newRequests, inCoordination, awaitingDoctor, awaitingPatient, confirmed, cancelled);

        List<NeedsAttentionItem> needsAttention = open.stream()
                .limit(NEEDS_ATTENTION_LIMIT)
                .map(r -> new NeedsAttentionItem(
                        r.getId().toString(),
                        r.getPatientName(),
                        r.getDepartment(),
                        toLocalDateTime(effectiveRequestedAt(r)),
                        r.getStatus().name(),
                        slaMinutesRemaining(r, now)))
                .toList();

        List<SlaAlertItem> slaAlerts = open.stream()
                .filter(r -> slaMinutesRemaining(r, now) < 0)
                .limit(SLA_ALERTS_LIMIT)
                .map(r -> new SlaAlertItem(
                        r.getId().toString(),
                        r.getPatientName(),
                        "No response for "
                                + Math.abs(slaMinutesRemaining(r, now))
                                + " minutes past the response window."))
                .toList();

        List<UpcomingAppointmentItem> upcomingAppointments = confirmedUpcoming.stream()
                .limit(UPCOMING_LIMIT)
                .map(r -> {
                    LocalDateTime dt = toLocalDateTime(effectiveScheduledAt(r));
                    return new UpcomingAppointmentItem(
                            r.getId().toString(),
                            dt.getDayOfMonth(),
                            dt.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ENGLISH),
                            r.getPatientName(),
                            acceptedDoctorName(r).orElse(r.getRequestedDoctorName()),
                            formatTime(dt));
                })
                .toList();

        return new DashboardSummaryResponse(kpis, pipeline, needsAttention, slaAlerts, upcomingAppointments);
    }

    private long countByStatus(List<AppointmentRequest> all, AppointmentRequestStatus status) {
        return all.stream().filter(r -> status.equals(r.getStatus())).count();
    }

    /** Minutes until adminSlaDeadline; negative once overdue. */
    private long slaMinutesRemaining(AppointmentRequest r, Instant now) {
        return java.time.Duration.between(now, r.getAdminSlaDeadline()).toMinutes();
    }

    /** The accepted doctor's proposed alternate slot if one exists, else the originally requested
     *  date/time — same fallback for both "when is this happening" and "when was it requested". */
    private Instant effectiveScheduledAt(AppointmentRequest r) {
        return acceptedAck(r)
                .filter(a -> a.getProposedDate() != null && a.getProposedTime() != null)
                .map(a -> a.getProposedDate().atTime(a.getProposedTime()).atZone(ZoneId.systemDefault()).toInstant())
                .orElseGet(() -> r.getRequestedDate().atTime(r.getRequestedTime())
                        .atZone(ZoneId.systemDefault()).toInstant());
    }

    private Instant effectiveRequestedAt(AppointmentRequest r) {
        return r.getRequestedDate().atTime(r.getRequestedTime()).atZone(ZoneId.systemDefault()).toInstant();
    }

    private java.util.Optional<String> acceptedDoctorName(AppointmentRequest r) {
        return acceptedAck(r).map(DoctorAcknowledgementRequest::getDoctorName);
    }

    private java.util.Optional<DoctorAcknowledgementRequest> acceptedAck(AppointmentRequest r) {
        return doctorAcknowledgementRequestRepository
                .findByAppointmentRequestIdAndStatus(r.getId(), DoctorAcknowledgementStatus.ACCEPTED)
                .stream()
                .findFirst();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private String formatTime(LocalDateTime dt) {
        return dt.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));
    }
}
