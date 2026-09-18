package com.doctrip.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.doctrip.backend.dto.AppointmentRequestDetailResponse;
import com.doctrip.backend.dto.DoctorAvailabilityResponse;
import com.doctrip.backend.dto.DoctorResponseRequest;
import com.doctrip.backend.dto.RegisterRequest;
import com.doctrip.backend.entity.AppointmentRequest;
import com.doctrip.backend.entity.AppointmentRequestStatus;
import com.doctrip.backend.entity.CoordinationDoctor;
import com.doctrip.backend.entity.DoctorAcknowledgementRequest;
import com.doctrip.backend.entity.DoctorAcknowledgementStatus;
import com.doctrip.backend.entity.Hospital;
import com.doctrip.backend.repository.AppointmentRequestRepository;
import com.doctrip.backend.repository.DoctorAcknowledgementRequestRepository;
import com.doctrip.backend.repository.CoordinationDoctorRepository;
import com.doctrip.backend.repository.HospitalRepository;
import com.doctrip.backend.service.AppointmentRequestService;
import com.doctrip.backend.service.CapturingNotificationService;
import com.doctrip.backend.service.CapturingOtpDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Hospital Appointment Coordination story — FR-01/FR-02/FR-04/FR-05/FR-07/FR-08/FR-10/FR-11/FR-03/
 * EC-12 and Patient Confirmation, exercised end to end against real Postgres-backed (H2 under the
 * {@code test} profile) repositories. {@code AdminSlaEnforcer}/{@code DoctorResponseTimeoutEnforcer}
 * are both {@code @Profile("!test")} (a real timer racing fixed test data would be flaky), so their
 * system-triggered behavior is tested by calling straight into the {@code AppointmentRequestService}
 * methods they'd otherwise call on a schedule.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentCoordinationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CapturingOtpDeliveryService otpDeliveryService;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private CoordinationDoctorRepository doctorRepository;

    @Autowired
    private AppointmentRequestRepository appointmentRequestRepository;

    @Autowired
    private DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository;

    @Autowired
    private AppointmentRequestService appointmentRequestService;

    @Autowired
    private CapturingNotificationService notificationService;

    private static final AtomicInteger COUNTER = new AtomicInteger();

    // ── fixtures ────────────────────────────────────────────────────────────────────────────

    private Hospital hospital() {
        int n = COUNTER.incrementAndGet();
        return hospitalRepository.save(Hospital.builder()
                .hospitalCode("HSP-TEST-" + n)
                .name("Test Hospital " + n)
                .city("Kochi")
                .beds(100)
                .build());
    }

    private CoordinationDoctor doctor(UUID hospitalId, String department, boolean available) {
        int n = COUNTER.incrementAndGet();
        return doctorRepository.save(CoordinationDoctor.builder()
                .id(UUID.randomUUID())
                .hospitalId(hospitalId)
                .name("Dr. Test " + n)
                .department(department)
                .specialty("General")
                .available(available)
                .build());
    }

    private AppointmentRequest requestFor(String patientId, UUID hospitalId, String department, String requestedDoctorName) {
        int n = COUNTER.incrementAndGet();
        return appointmentRequestRepository.save(AppointmentRequest.builder()
                .requestCode("APR-TEST-" + n)
                .patientId(patientId)
                .patientName("Test Patient")
                .patientCountry("UAE")
                .patientAge(30)
                .patientGender("Female")
                .hospitalId(hospitalId)
                .department(department)
                .requestedDoctorName(requestedDoctorName)
                .requestedDate(LocalDate.now().plusDays(7))
                .requestedTime(LocalTime.of(10, 0))
                .healthcareRequirement("Test requirement.")
                .status(AppointmentRequestStatus.REQUESTED)
                .build());
    }

    /** Registers + OTP-verifies a fresh patient, returning {patientId, accessToken}. */
    private record AuthedPatient(String patientId, String accessToken) {}

    /** Mobile is generated, not passed in — avoids collisions with other test classes' own hardcoded
     * numbers sharing the same H2 instance across the whole Maven test run. */
    private AuthedPatient registerAndVerifyPatient(String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Coordination Test Patient");
        req.setEmail(email);
        req.setMobile("77" + String.format("%08d", COUNTER.incrementAndGet()));
        req.setPassword("Str0ng!Pass1");
        req.setAgreeToTerms(true);

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(body, Map.class);
        String patientId = (String) registerResponse.get("patientId");
        String otp = otpDeliveryService.lastOtpFor(email);

        String verifyBody = mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> verifyResponse = objectMapper.readValue(verifyBody, Map.class);
        return new AuthedPatient(patientId, (String) verifyResponse.get("accessToken"));
    }

    private List<DoctorAcknowledgementRequest> acksFor(UUID requestId) {
        return doctorAcknowledgementRequestRepository.findByAppointmentRequestIdOrderBySentAtAsc(requestId);
    }

    // ── FR-01 ───────────────────────────────────────────────────────────────────────────────

    @Test
    void adminCanReviewRequestFieldsAndListByHospital() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr01@example.com");
        Hospital h = hospital();
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", "Dr. Anil Kumar");

        mockMvc.perform(get("/api/admin/appointment-requests").param("hospitalId", h.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestCode").value(r.getRequestCode()));

        mockMvc.perform(get("/api/admin/appointment-requests/" + r.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Test Patient"))
                .andExpect(jsonPath("$.department").value("Cardiology"))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    // ── FR-04 — requested doctor available ─────────────────────────────────────────────────

    @Test
    void requestedDoctorAvailableGetsSingleAcknowledgement() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr04@example.com");
        Hospital h = hospital();
        CoordinationDoctor requested = doctor(h.getId(), "Cardiology", true);
        doctor(h.getId(), "Cardiology", true); // another eligible doctor who must NOT be contacted
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", requested.getName());

        String body = mockMvc.perform(get("/api/admin/appointment-requests/" + r.getId() + "/doctor-availability"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        DoctorAvailabilityResponse availability = objectMapper.readValue(body, DoctorAvailabilityResponse.class);
        assertThat(availability.requestedDoctorAvailable()).isTrue();

        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOCTOR_COORDINATION"))
                .andExpect(jsonPath("$.coordination.length()").value(1))
                .andExpect(jsonPath("$.coordination[0].doctorName").value(requested.getName()));
    }

    @Test
    void startingCoordinationTwiceIsRejected() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr04b@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);

        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isConflict());
    }

    // ── FR-05 — fan-out / no eligible doctors ──────────────────────────────────────────────

    @Test
    void noPreferenceFansOutSimultaneouslyToEveryEligibleDoctor() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr05@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true);
        doctor(h.getId(), "Cardiology", true);
        doctor(h.getId(), "Cardiology", false); // unavailable — must be excluded
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);

        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coordination.length()").value(2));

        List<DoctorAcknowledgementRequest> acks = acksFor(r.getId());
        assertThat(acks).hasSize(2);
        assertThat(acks.get(0).getSentAt()).isEqualTo(acks.get(1).getSentAt());
    }

    @Test
    void noEligibleDoctorsRejectsStartingCoordination() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr05b@example.com");
        Hospital h = hospital();
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Neurology", null);

        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isConflict());
    }

    // ── FR-07 — atomic accept, first wins, late response rejected ─────────────────────────

    @Test
    void firstAcceptanceWinsAndClosesOthersLateResponseRejected() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr07@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true);
        doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());

        List<DoctorAcknowledgementRequest> acks = acksFor(r.getId());
        UUID winner = acks.get(0).getId();
        UUID loser = acks.get(1).getId();

        mockMvc.perform(post("/api/doctor/acknowledgements/" + winner + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorResponseRequest("ACCEPT", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOCTOR_SELECTED"));

        // AC-14/EC-05 — the loser's own (still PENDING at the time) acceptance must fail cleanly.
        mockMvc.perform(post("/api/doctor/acknowledgements/" + loser + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorResponseRequest("ACCEPT", null, null))))
                .andExpect(status().isConflict());

        AppointmentRequest refreshed = appointmentRequestRepository.findById(r.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(AppointmentRequestStatus.DOCTOR_SELECTED);
        assertThat(doctorAcknowledgementRequestRepository.findById(loser).orElseThrow().getStatus())
                .isEqualTo(DoctorAcknowledgementStatus.CLOSED);
    }

    // ── FR-08 — alternative slots, earliest wins ────────────────────────────────────────────

    @Test
    void earliestValidAlternativeIsSelectedWhenResolved() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr08@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true);
        doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());

        List<DoctorAcknowledgementRequest> acks = acksFor(r.getId());

        proposeAlternative(acks.get(0).getId(), LocalDate.now().plusDays(10), LocalTime.of(10, 0));
        proposeAlternative(acks.get(1).getId(), LocalDate.now().plusDays(8), LocalTime.of(14, 0)); // earlier

        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/resolve-alternative"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOCTOR_SELECTED"))
                .andExpect(jsonPath("$.securedDoctorName").value(acks.get(1).getDoctorName()));
    }

    private void proposeAlternative(UUID ackId, LocalDate date, LocalTime time) throws Exception {
        mockMvc.perform(post("/api/doctor/acknowledgements/" + ackId + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorResponseRequest("PROPOSE_ALTERNATIVE", date, time))))
                .andExpect(status().isOk());
    }

    // ── FR-10/EC-10 — all doctors reject ────────────────────────────────────────────────────

    @Test
    void allDoctorsRejectingEscalates() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr10@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true);
        doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());

        List<DoctorAcknowledgementRequest> acks = acksFor(r.getId());
        for (DoctorAcknowledgementRequest ack : acks) {
            mockMvc.perform(post("/api/doctor/acknowledgements/" + ack.getId() + "/respond")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DoctorResponseRequest("REJECT", null, null))))
                    .andExpect(status().isOk());
        }

        assertThat(appointmentRequestRepository.findById(r.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentRequestStatus.ESCALATED);
    }

    @Test
    void rejectingWhileOthersStillPendingKeepsCoordinationOpen() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("ac07@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true);
        doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());

        List<DoctorAcknowledgementRequest> acks = acksFor(r.getId());
        mockMvc.perform(post("/api/doctor/acknowledgements/" + acks.get(0).getId() + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorResponseRequest("REJECT", null, null))))
                .andExpect(status().isOk());

        assertThat(appointmentRequestRepository.findById(r.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentRequestStatus.DOCTOR_COORDINATION);
        assertThat(doctorAcknowledgementRequestRepository.findById(acks.get(1).getId()).orElseThrow().getStatus())
                .isEqualTo(DoctorAcknowledgementStatus.PENDING);
    }

    // ── FR-11 — Patient cancellation ────────────────────────────────────────────────────────

    @Test
    void cancellationRequiresOwnershipAndIsIdempotent() throws Exception {
        AuthedPatient owner = registerAndVerifyPatient("fr11a@example.com");
        AuthedPatient other = registerAndVerifyPatient("fr11b@example.com");
        Hospital h = hospital();
        AppointmentRequest r = requestFor(owner.patientId(), h.getId(), "Cardiology", null);

        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/cancel"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/cancel")
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/cancel")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/cancel")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void cancellationClosesAcceptedAckAndBlocksLateAcceptance() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("ac16@example.com");
        Hospital h = hospital();
        CoordinationDoctor d = doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", d.getName());
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());
        UUID ackId = acksFor(r.getId()).get(0).getId();

        // AC-16/BR-18 — cancellation commits before the doctor's acceptance arrives.
        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/cancel")
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/doctor/acknowledgements/" + ackId + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorResponseRequest("ACCEPT", null, null))))
                .andExpect(status().isConflict());

        assertThat(appointmentRequestRepository.findById(r.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentRequestStatus.CANCELLED);
    }

    // ── Patient Confirmation ────────────────────────────────────────────────────────────────

    @Test
    void confirmationOnlyValidFromDoctorSelectedAndBlocksLaterCancellation() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("confirm@example.com");
        Hospital h = hospital();
        CoordinationDoctor d = doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", d.getName());

        // Too early — still REQUESTED.
        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/confirm")
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());
        UUID ackId = acksFor(r.getId()).get(0).getId();
        mockMvc.perform(post("/api/doctor/acknowledgements/" + ackId + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorResponseRequest("ACCEPT", null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/confirm")
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Double confirm rejected.
        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/confirm")
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isConflict());

        // AC-18/BR-20 — once Confirmed, FR-11 no longer applies.
        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/cancel")
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isConflict());
    }

    // ── FR-03 — Admin SLA system takeover ───────────────────────────────────────────────────

    @Test
    void systemTakesOverAfterAdminSlaBreach() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr03a@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);
        r.setAdminSlaDeadline(Instant.now().minusSeconds(60));
        appointmentRequestRepository.save(r);

        appointmentRequestService.systemTakeOverIfStillRequested(r.getId());

        AppointmentRequest refreshed = appointmentRequestRepository.findById(r.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(AppointmentRequestStatus.DOCTOR_COORDINATION);
        assertThat(acksFor(r.getId())).hasSize(1);
    }

    @Test
    void systemTakeoverEscalatesDirectlyWhenNoEligibleDoctors() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr03b@example.com");
        Hospital h = hospital();
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Neurology", null);
        r.setAdminSlaDeadline(Instant.now().minusSeconds(60));
        appointmentRequestRepository.save(r);

        appointmentRequestService.systemTakeOverIfStillRequested(r.getId());

        assertThat(appointmentRequestRepository.findById(r.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentRequestStatus.ESCALATED);
    }

    @Test
    void systemDoesNotTakeOverARequestAlreadyHandled() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("fr03c@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());

        // Admin already started it — the "system" pass must be a no-op (BR-03/EC-13), not re-fan-out.
        appointmentRequestService.systemTakeOverIfStillRequested(r.getId());

        assertThat(acksFor(r.getId())).hasSize(1);
    }

    // ── EC-12 — doctor response-window timeout ─────────────────────────────────────────────

    @Test
    void overdueAcknowledgementIsClosedAndExhaustionEscalates() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("ec12a@example.com");
        Hospital h = hospital();
        CoordinationDoctor d = doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", d.getName());
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());

        DoctorAcknowledgementRequest ack = acksFor(r.getId()).get(0);
        ack.setResponseDeadline(Instant.now().minusSeconds(60));
        doctorAcknowledgementRequestRepository.save(ack);

        appointmentRequestService.expireOverdueAcknowledgements();

        assertThat(doctorAcknowledgementRequestRepository.findById(ack.getId()).orElseThrow().getStatus())
                .isEqualTo(DoctorAcknowledgementStatus.CLOSED);
        assertThat(appointmentRequestRepository.findById(r.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentRequestStatus.ESCALATED);

        mockMvc.perform(post("/api/doctor/acknowledgements/" + ack.getId() + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorResponseRequest("ACCEPT", null, null))))
                .andExpect(status().isConflict());
    }

    @Test
    void partialTimeoutLeavesCoordinationOpenWhileOtherDoctorStillPending() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("ec12b@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true);
        doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());

        List<DoctorAcknowledgementRequest> acks = acksFor(r.getId());
        DoctorAcknowledgementRequest expiring = acks.get(0);
        expiring.setResponseDeadline(Instant.now().minusSeconds(60));
        doctorAcknowledgementRequestRepository.save(expiring);

        appointmentRequestService.expireOverdueAcknowledgements();

        assertThat(doctorAcknowledgementRequestRepository.findById(expiring.getId()).orElseThrow().getStatus())
                .isEqualTo(DoctorAcknowledgementStatus.CLOSED);
        assertThat(doctorAcknowledgementRequestRepository.findById(acks.get(1).getId()).orElseThrow().getStatus())
                .isEqualTo(DoctorAcknowledgementStatus.PENDING);
        assertThat(appointmentRequestRepository.findById(r.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentRequestStatus.DOCTOR_COORDINATION);
    }

    // ── EC-01/EC-02 — doctor becomes unavailable before accepting ──────────────────────────────

    @Test
    void acceptanceIsRejectedIfDoctorBecameUnavailableSinceTheAckWasSent() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("ec01@example.com");
        Hospital h = hospital();
        CoordinationDoctor d = doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", d.getName());
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());
        UUID ackId = acksFor(r.getId()).get(0).getId();

        // Doctor's availability flips after the ack was sent but before they respond.
        d.setAvailable(false);
        doctorRepository.save(d);

        mockMvc.perform(post("/api/doctor/acknowledgements/" + ackId + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorResponseRequest("ACCEPT", null, null))))
                .andExpect(status().isConflict());

        assertThat(doctorAcknowledgementRequestRepository.findById(ackId).orElseThrow().getStatus())
                .isEqualTo(DoctorAcknowledgementStatus.REJECTED);
        // Sole doctor, now rejected -> exhausted -> escalated (FR-10), same as any other rejection.
        assertThat(appointmentRequestRepository.findById(r.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentRequestStatus.ESCALATED);
    }

    // ── EC-09 — a proposing doctor becomes unavailable before the admin resolves alternatives ──

    @Test
    void resolveAlternativeSkipsAnEarlierProposalFromANowUnavailableDoctor() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("ec09@example.com");
        Hospital h = hospital();
        CoordinationDoctor early = doctor(h.getId(), "Cardiology", true);
        CoordinationDoctor later = doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());

        List<DoctorAcknowledgementRequest> acks = acksFor(r.getId());
        DoctorAcknowledgementRequest earlyAck =
                acks.get(0).getDoctorId().equals(early.getId()) ? acks.get(0) : acks.get(1);
        DoctorAcknowledgementRequest laterAck =
                acks.get(0).getDoctorId().equals(later.getId()) ? acks.get(0) : acks.get(1);

        proposeAlternative(earlyAck.getId(), LocalDate.now().plusDays(5), LocalTime.of(9, 0)); // earliest
        proposeAlternative(laterAck.getId(), LocalDate.now().plusDays(9), LocalTime.of(9, 0)); // later

        // The earliest-proposing doctor becomes unavailable before the admin resolves.
        early.setAvailable(false);
        doctorRepository.save(early);

        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/resolve-alternative"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securedDoctorName").value(later.getName()));
    }

    @Test
    void resolveAlternativeConflictsWhenEveryProposingDoctorIsUnavailable() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("ec09b@example.com");
        Hospital h = hospital();
        CoordinationDoctor d = doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());
        UUID ackId = acksFor(r.getId()).get(0).getId();
        proposeAlternative(ackId, LocalDate.now().plusDays(5), LocalTime.of(9, 0));

        d.setAvailable(false);
        doctorRepository.save(d);

        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/resolve-alternative"))
                .andExpect(status().isConflict());
    }

    // ── FR-09 — notification delivery ──────────────────────────────────────────────────────────

    @Test
    void notifiesPatientWhenDoctorAcceptsAndBothPartiesWhenPatientConfirms() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("notify1@example.com");
        Hospital h = hospital();
        CoordinationDoctor d = doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", d.getName());
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());
        assertThat(notificationService.hasEventFor("DOCTOR", d.getId().toString(), "doctor_acknowledgement_request"))
                .isTrue();

        UUID ackId = acksFor(r.getId()).get(0).getId();
        mockMvc.perform(post("/api/doctor/acknowledgements/" + ackId + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorResponseRequest("ACCEPT", null, null))))
                .andExpect(status().isOk());
        assertThat(notificationService.hasEventFor("PATIENT", patient.patientId(), "doctor_accepted")).isTrue();

        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/confirm")
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isOk());
        assertThat(notificationService.hasEventFor("DOCTOR", d.getId().toString(), "patient_confirmed")).isTrue();
        assertThat(notificationService.hasEventFor("ADMIN", h.getId().toString(), "patient_confirmed")).isTrue();
    }

    @Test
    void notifiesPatientAndAdminWhenCoordinationIsExhausted() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("notify2@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", null);
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());
        UUID ackId = acksFor(r.getId()).get(0).getId();

        mockMvc.perform(post("/api/doctor/acknowledgements/" + ackId + "/respond")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DoctorResponseRequest("REJECT", null, null))))
                .andExpect(status().isOk());

        assertThat(notificationService.hasEventFor("PATIENT", patient.patientId(), "coordination_failed")).isTrue();
        assertThat(notificationService.hasEventFor("ADMIN", h.getId().toString(), "coordination_failed")).isTrue();
    }

    @Test
    void notifiesAdminAndActiveDoctorOnPatientCancellation() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("notify3@example.com");
        Hospital h = hospital();
        CoordinationDoctor d = doctor(h.getId(), "Cardiology", true);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology", d.getName());
        mockMvc.perform(post("/api/admin/appointment-requests/" + r.getId() + "/coordination/start"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/patient/appointment-requests/" + r.getId() + "/cancel")
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isOk());

        assertThat(notificationService.hasEventFor("ADMIN", h.getId().toString(), "patient_cancelled")).isTrue();
        assertThat(notificationService.hasEventFor("DOCTOR", d.getId().toString(), "patient_cancelled")).isTrue();
    }
}
