package com.doctrip.backend;

import com.doctrip.backend.dto.RegisterRequest;
import com.doctrip.backend.service.CapturingOtpDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CapturingOtpDeliveryService otpDeliveryService;

    private RegisterRequest validRegistration(String email, String mobile) {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane Doe");
        req.setEmail(email);
        req.setMobile(mobile);
        req.setPassword("Str0ng!Pass1");
        req.setAgreeToTerms(true);
        return req;
    }

    @Test
    void registerWithWeakPasswordIsRejected() throws Exception {
        RegisterRequest req = validRegistration("weakpw@example.com", "9000000001");
        req.setPassword("weak");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        RegisterRequest first = validRegistration("dup@example.com", "9000000002");
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest second = validRegistration("dup@example.com", "9000000003");
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationThenValidOtpEstablishesSession() throws Exception {
        RegisterRequest req = validRegistration("verify.me@example.com", "9000000004");

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(responseBody, Map.class);
        String patientId = (String) registerResponse.get("patientId");
        String otp = otpDeliveryService.lastOtpFor("verify.me@example.com");

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType("application/json")
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.patient.status").value("active"))
                .andExpect(jsonPath("$.patient.emailVerified").value(true));
    }

    @Test
    void incorrectOtpIsRejectedAndDoesNotAuthenticate() throws Exception {
        RegisterRequest req = validRegistration("badotp@example.com", "9000000005");

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(responseBody, Map.class);
        String patientId = (String) registerResponse.get("patientId");

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType("application/json")
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\",\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otpAttemptLimitIsEnforced() throws Exception {
        RegisterRequest req = validRegistration("lockout@example.com", "9000000006");

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(responseBody, Map.class);
        String patientId = (String) registerResponse.get("patientId");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/otp/verify")
                            .contentType("application/json")
                            .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\",\"otp\":\"000000\"}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType("application/json")
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\",\"otp\":\"000000\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void resendOtpBeforeCooldownIsRateLimited() throws Exception {
        RegisterRequest req = validRegistration("resend@example.com", "9000000007");
        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(responseBody, Map.class);
        String patientId = (String) registerResponse.get("patientId");

        // Registration already issued one OTP; resending immediately must hit the cooldown (BR-06/AC-08).
        mockMvc.perform(post("/api/auth/otp/resend")
                        .contentType("application/json")
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
