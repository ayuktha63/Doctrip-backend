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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-10/BR-11 — Mobile and Website authenticate through the same patient identity, and
 * patient-specific information stays associated with that one identity regardless of channel.
 * There's no server-side notion of "platform" to configure (any HTTP client can call any
 * endpoint) — what this proves is that every authentication path (password login, OTP login)
 * resolves to the *same* Patient row and that a token from either path sees identical data via
 * GET /api/patients/me, which is what "same identity across channels" actually means here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrossPlatformIdentityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CapturingOtpDeliveryService otpDeliveryService;

    @Test
    void passwordLoginFromOneChannelAndOtpLoginFromAnotherResolveToTheSamePatient() throws Exception {
        String email = "cross.platform@example.com";
        String mobile = "9400000001";
        String password = "Str0ng!Pass1";

        RegisterRequest req = new RegisterRequest();
        req.setFullName("Cross Platform Patient");
        req.setEmail(email);
        req.setMobile(mobile);
        req.setPassword(password);

        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(registerBody, Map.class);
        String patientId = (String) registerResponse.get("patientId");
        String registerOtp = otpDeliveryService.lastOtpFor(email);

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType("application/json")
                        .header("User-Agent", "DocTrip-Website/1.0")
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + registerOtp + "\"}"))
                .andExpect(status().isOk());

        // "Website" channel: password login.
        String webLoginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .header("User-Agent", "DocTrip-Website/1.0")
                        .content("{\"loginId\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> webAuth = objectMapper.readValue(webLoginBody, Map.class);
        String webAccessToken = (String) webAuth.get("accessToken");

        // "Mobile app" channel: OTP login, identified by mobile number instead of email.
        String otpRequestBody = mockMvc.perform(post("/api/auth/login/otp")
                        .contentType("application/json")
                        .header("User-Agent", "DocTrip-MobileApp/1.0")
                        .content("{\"loginId\":\"" + mobile + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> otpChallenge = objectMapper.readValue(otpRequestBody, Map.class);
        String mobileLoginOtp = otpDeliveryService.lastOtpFor(email); // channel is always EMAIL in this slice

        String mobileLoginBody = mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType("application/json")
                        .header("User-Agent", "DocTrip-MobileApp/1.0")
                        .content("{\"patientId\":\"" + otpChallenge.get("patientId") + "\",\"purpose\":\"LOGIN\",\"otp\":\"" + mobileLoginOtp + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> mobileAuth = objectMapper.readValue(mobileLoginBody, Map.class);
        String mobileAccessToken = (String) mobileAuth.get("accessToken");

        assertThat(((Map<?, ?>) webAuth.get("patient")).get("id"))
                .isEqualTo(((Map<?, ?>) mobileAuth.get("patient")).get("id"))
                .isEqualTo(patientId);

        // Both tokens must resolve, via the actual protected endpoint, to the same patient data —
        // not just the same id embedded in two separately-constructed responses.
        String webMeBody = mockMvc.perform(get("/api/patients/me")
                        .header("Authorization", "Bearer " + webAccessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String mobileMeBody = mockMvc.perform(get("/api/patients/me")
                        .header("Authorization", "Bearer " + mobileAccessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(webMeBody).isEqualTo(mobileMeBody);
    }
}
