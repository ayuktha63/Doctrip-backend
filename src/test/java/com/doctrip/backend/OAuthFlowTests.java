package com.doctrip.backend;

import com.doctrip.backend.dto.AuthResponse;
import com.doctrip.backend.dto.OAuthCompleteRequest;
import com.doctrip.backend.dto.RegisterRequest;
import com.doctrip.backend.exception.UnauthorizedException;
import com.doctrip.backend.repository.PatientRepository;
import com.doctrip.backend.service.AuthService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-06/FR-07 — exercises AuthService.completeOAuth(...) directly (the business logic reached by
 * OAuth2LoginSuccessHandler after Spring Security itself validates a real Google/Apple handshake).
 * There is deliberately no public HTTP endpoint that accepts these claims directly — see
 * OAuthCompleteRequest's javadoc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuthFlowTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CapturingOtpDeliveryService otpDeliveryService;

    @Test
    void newGoogleIdentityCreatesActivePatient() {
        AuthResponse response = authService.completeOAuth(OAuthCompleteRequest.builder()
                .provider("google")
                .externalId("google-sub-1")
                .email("oauth.new@example.com")
                .name("OAuth Newcomer")
                .emailVerified(true)
                .build());

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getPatient().getStatus()).isEqualTo("active");
        assertThat(response.getPatient().isEmailVerified()).isTrue();
        assertThat(patientRepository.existsByEmail("oauth.new@example.com")).isTrue();
    }

    @Test
    void sameExternalIdentityResolvesToTheSamePatientOnRelogin() {
        AuthResponse first = authService.completeOAuth(OAuthCompleteRequest.builder()
                .provider("google")
                .externalId("google-sub-2")
                .email("oauth.repeat@example.com")
                .name("OAuth Repeat")
                .emailVerified(true)
                .build());

        AuthResponse second = authService.completeOAuth(OAuthCompleteRequest.builder()
                .provider("google")
                .externalId("google-sub-2")
                .email("oauth.repeat@example.com")
                .name("OAuth Repeat")
                .emailVerified(true)
                .build());

        assertThat(second.getPatient().getId()).isEqualTo(first.getPatient().getId());
    }

    @Test
    void oauthLinksToExistingPatientAccountByVerifiedEmail() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Existing Patient");
        req.setEmail("existing.patient@example.com");
        req.setMobile("9200000001");
        req.setPassword("Str0ng!Pass1");

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(responseBody, Map.class);
        String patientId = (String) registerResponse.get("patientId");
        String otp = otpDeliveryService.lastOtpFor("existing.patient@example.com");
        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType("application/json")
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk());

        AuthResponse oauthResponse = authService.completeOAuth(OAuthCompleteRequest.builder()
                .provider("apple")
                .externalId("apple-sub-1")
                .email("existing.patient@example.com")
                .name("Existing Patient")
                .emailVerified(true)
                .build());

        assertThat(oauthResponse.getPatient().getId()).isEqualTo(patientId);
    }

    @Test
    void unverifiedProviderEmailIsRejected() {
        assertThatThrownBy(() -> authService.completeOAuth(OAuthCompleteRequest.builder()
                .provider("google")
                .externalId("google-sub-unverified")
                .email("unverified@example.com")
                .name("Unverified")
                .emailVerified(false)
                .build()))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(patientRepository.existsByEmail("unverified@example.com")).isFalse();
    }
}
