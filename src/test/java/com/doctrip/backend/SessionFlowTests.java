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

/** FR-08 (access/refresh session management) and FR-09 (logout). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CapturingOtpDeliveryService otpDeliveryService;

    private Map<String, Object> registerAndVerify(String email, String mobile) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane Doe");
        req.setEmail(email);
        req.setMobile(mobile);
        req.setPassword("Str0ng!Pass1");
        req.setAgreeToTerms(true);

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(responseBody, Map.class);
        String patientId = (String) registerResponse.get("patientId");
        String otp = otpDeliveryService.lastOtpFor(email);

        String verifyBody = mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType("application/json")
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> authResponse = objectMapper.readValue(verifyBody, Map.class);
        return authResponse;
    }

    @Test
    void registrationIssuesBothAccessAndRefreshTokens() throws Exception {
        Map<String, Object> auth = registerAndVerify("session.tokens@example.com", "9300000001");
        org.assertj.core.api.Assertions.assertThat(auth.get("accessToken")).isNotNull();
        org.assertj.core.api.Assertions.assertThat(auth.get("refreshToken")).isNotNull();
    }

    @Test
    void refreshTokenExchangesForNewAccessTokenAndRotates() throws Exception {
        Map<String, Object> auth = registerAndVerify("session.refresh@example.com", "9300000002");
        String refreshToken = (String) auth.get("refreshToken");

        String responseBody = mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> refreshed = objectMapper.readValue(responseBody, Map.class);
        String newRefreshToken = (String) refreshed.get("refreshToken");
        org.assertj.core.api.Assertions.assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // The used refresh token was rotated out — reusing it must fail (BR-12 / reuse detection).
        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRefreshTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"not-a-real-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesAccessTokenAndRefreshToken() throws Exception {
        Map<String, Object> auth = registerAndVerify("session.logout@example.com", "9300000003");
        String accessToken = (String) auth.get("accessToken");
        String refreshToken = (String) auth.get("refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // The just-revoked access token must no longer grant access to a protected endpoint.
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        // Its refresh token must be revoked too — logout ends the whole session, not just one token.
        mockMvc.perform(post("/api/auth/token/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutAuthenticationIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
