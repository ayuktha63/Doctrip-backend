package com.doctrip.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.doctrip.backend.dto.RegisterRequest;
import com.doctrip.backend.entity.AppointmentRequest;
import com.doctrip.backend.entity.AppointmentRequestStatus;
import com.doctrip.backend.entity.CoordinationDoctor;
import com.doctrip.backend.entity.DoctorAcknowledgementRequest;
import com.doctrip.backend.entity.DoctorAcknowledgementStatus;
import com.doctrip.backend.entity.Hospital;
import com.doctrip.backend.repository.AppointmentRequestRepository;
import com.doctrip.backend.repository.CoordinationDoctorRepository;
import com.doctrip.backend.repository.DoctorAcknowledgementRequestRepository;
import com.doctrip.backend.repository.HospitalRepository;
import com.doctrip.backend.security.JwtService;
import com.doctrip.backend.service.CapturingDoctorAcknowledgementMailService;
import com.doctrip.backend.service.CapturingOtpDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Story 7 — the doctor's email-only acknowledgement channel (the piece this story added on top of
 * DT-6's already-tested coordination state machine): the outbound mail-service call fired by
 * {@code beginCoordination}, and the one-click Accept/Reject GET links. The IMAP reply-polling
 * itself is exercised through the same {@code respondToAcknowledgement} call the poller makes
 * (already covered by {@link AppointmentCoordinationTests#earliestValidAlternativeIsSelectedWhenResolved}
 * and friends) — connecting to a real mailbox is deliberately out of scope for an automated suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Story7EmailAcknowledgementTests {

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
    private CapturingDoctorAcknowledgementMailService mailService;

    @Autowired
    private JwtService jwtService;

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private String hospitalAdminAuth(Hospital h) {
        return "Bearer " + jwtService.generateHospitalAdminToken(UUID.randomUUID(), h.getId());
    }

    private Hospital hospital() {
        int n = COUNTER.incrementAndGet();
        return hospitalRepository.save(Hospital.builder()
                .hospitalCode("HSP-S7-" + n)
                .name("Story7 Test Hospital " + n)
                .city("Kochi")
                .beds(100)
                .build());
    }

    private CoordinationDoctor doctor(UUID hospitalId, String department, boolean available, String email) {
        int n = COUNTER.incrementAndGet();
        return doctorRepository.save(CoordinationDoctor.builder()
                .id(UUID.randomUUID())
                .hospitalId(hospitalId)
                .name("Dr. Story7 " + n)
                .department(department)
                .specialty("General")
                .available(available)
                .email(email)
                .build());
    }

    private AppointmentRequest requestFor(String patientId, UUID hospitalId, String department) {
        int n = COUNTER.incrementAndGet();
        return appointmentRequestRepository.save(AppointmentRequest.builder()
                .requestCode("APR-S7-" + n)
                .patientId(patientId)
                .patientName("Story7 Patient")
                .patientCountry("India")
                .patientAge(35)
                .patientGender("Male")
                .hospitalId(hospitalId)
                .department(department)
                .requestedDate(LocalDate.now().plusDays(3))
                .requestedTime(LocalTime.of(10, 0))
                .healthcareRequirement("Story7 requirement.")
                .status(AppointmentRequestStatus.REQUESTED)
                .build());
    }

    private record AuthedPatient(String patientId, String accessToken) {}

    private AuthedPatient registerAndVerifyPatient(String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Story7 Test Patient");
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

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk());
        return new AuthedPatient(patientId, null);
    }

    private List<DoctorAcknowledgementRequest> acksFor(UUID requestId) {
        return doctorAcknowledgementRequestRepository.findByAppointmentRequestIdOrderBySentAtAsc(requestId);
    }

    // ── FR-01 — outbound mail is triggered with a real Message-ID on coordination start ────────

    @Test
    void startingCoordinationSendsAckEmailWithMessageIdPersisted() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("s7-mail1@example.com");
        Hospital h = hospital();
        CoordinationDoctor d = doctor(h.getId(), "Cardiology", true, "doctor1@example.com");
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology");

        mockMvc.perform(post("/api/hospital-admin/appointment-requests/" + r.getId() + "/coordination/start")
                .header("Authorization", hospitalAdminAuth(h)))
                .andExpect(status().isOk());

        DoctorAcknowledgementRequest ack = acksFor(r.getId()).get(0);
        assertThat(mailService.sent())
                .anyMatch(m -> m.ackId().equals(ack.getId())
                        && m.appointmentRequestId().equals(r.getId())
                        && m.doctorEmail().equals("doctor1@example.com")
                        && m.messageId() != null
                        && !m.messageId().isBlank());

        DoctorAcknowledgementRequest reloaded = doctorAcknowledgementRequestRepository.findById(ack.getId()).orElseThrow();
        assertThat(reloaded.getEmailMessageId()).isNotBlank();
        assertThat(doctorAcknowledgementRequestRepository.findByEmailMessageId(reloaded.getEmailMessageId()))
                .isPresent();
    }

    @Test
    void doctorWithNoEmailOnFileIsSkippedWithoutError() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("s7-mail2@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true, null);
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology");

        mockMvc.perform(post("/api/hospital-admin/appointment-requests/" + r.getId() + "/coordination/start")
                .header("Authorization", hospitalAdminAuth(h)))
                .andExpect(status().isOk());

        DoctorAcknowledgementRequest ack = acksFor(r.getId()).get(0);
        assertThat(mailService.sent()).noneMatch(m -> m.ackId().equals(ack.getId()));
        assertThat(doctorAcknowledgementRequestRepository.findById(ack.getId()).orElseThrow().getEmailMessageId())
                .isNull();
    }

    // ── One-click Accept/Reject links (respond-link) ─────────────────────────────────────────

    @Test
    void acceptLinkSecuresTheAppointmentAndReturnsHtml() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("s7-accept@example.com");
        Hospital h = hospital();
        CoordinationDoctor d = doctor(h.getId(), "Cardiology", true, "doctor2@example.com");
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology");
        mockMvc.perform(post("/api/hospital-admin/appointment-requests/" + r.getId() + "/coordination/start")
                .header("Authorization", hospitalAdminAuth(h)))
                .andExpect(status().isOk());
        UUID ackId = acksFor(r.getId()).get(0).getId();

        mockMvc.perform(get("/api/doctor/acknowledgements/" + ackId + "/respond-link").param("action", "ACCEPT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("accepted")));

        assertThat(appointmentRequestRepository.findById(r.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentRequestStatus.DOCTOR_SELECTED);
        assertThat(doctorAcknowledgementRequestRepository.findById(ackId).orElseThrow().getStatus())
                .isEqualTo(DoctorAcknowledgementStatus.ACCEPTED);
    }

    @Test
    void rejectLinkClosesTheAckAndReturnsHtml() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("s7-reject@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true, "doctor3@example.com");
        doctor(h.getId(), "Cardiology", true, "doctor4@example.com");
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology");
        mockMvc.perform(post("/api/hospital-admin/appointment-requests/" + r.getId() + "/coordination/start")
                .header("Authorization", hospitalAdminAuth(h)))
                .andExpect(status().isOk());
        UUID ackId = acksFor(r.getId()).get(0).getId();

        mockMvc.perform(get("/api/doctor/acknowledgements/" + ackId + "/respond-link").param("action", "REJECT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("declined")));

        assertThat(doctorAcknowledgementRequestRepository.findById(ackId).orElseThrow().getStatus())
                .isEqualTo(DoctorAcknowledgementStatus.REJECTED);
        // Sibling ack still pending — coordination stays open (FR-05).
        assertThat(appointmentRequestRepository.findById(r.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentRequestStatus.DOCTOR_COORDINATION);
    }

    @Test
    void clickingAnAlreadyResolvedLinkShowsAFriendlyPageInsteadOfAnError() throws Exception {
        AuthedPatient patient = registerAndVerifyPatient("s7-stale@example.com");
        Hospital h = hospital();
        doctor(h.getId(), "Cardiology", true, "doctor5@example.com");
        AppointmentRequest r = requestFor(patient.patientId(), h.getId(), "Cardiology");
        mockMvc.perform(post("/api/hospital-admin/appointment-requests/" + r.getId() + "/coordination/start")
                .header("Authorization", hospitalAdminAuth(h)))
                .andExpect(status().isOk());
        UUID ackId = acksFor(r.getId()).get(0).getId();

        mockMvc.perform(get("/api/doctor/acknowledgements/" + ackId + "/respond-link").param("action", "ACCEPT"))
                .andExpect(status().isOk());

        // Clicking Accept again (e.g. doctor double-clicks, or clicks Reject after already accepting)
        // must not surface a raw 409/JSON error — it's a link opened in a browser.
        mockMvc.perform(get("/api/doctor/acknowledgements/" + ackId + "/respond-link").param("action", "REJECT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("no longer valid")));
    }

    @Test
    void respondLinkForAnUnknownIdShowsAFriendlyPage() throws Exception {
        mockMvc.perform(get("/api/doctor/acknowledgements/" + UUID.randomUUID() + "/respond-link")
                        .param("action", "ACCEPT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("no longer valid")));
    }
}
