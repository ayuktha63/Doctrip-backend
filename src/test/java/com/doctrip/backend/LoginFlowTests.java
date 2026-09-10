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

/** FR-04 (password login) and FR-05 (login OTP), including the Redis-backed (in-memory fake in tests) rate limits. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CapturingOtpDeliveryService otpDeliveryService;

    private String registerAndVerify(String email, String mobile) throws Exception {
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

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType("application/json")
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"REGISTER\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk());
        return patientId;
    }

    @Test
    void loginWithCorrectPasswordSucceeds() throws Exception {
        registerAndVerify("login.ok@example.com", "9100000001");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"loginId\":\"login.ok@example.com\",\"password\":\"Str0ng!Pass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.patient.email").value("login.ok@example.com"));
    }

    @Test
    void loginWithMobileIdentifierSucceeds() throws Exception {
        registerAndVerify("login.mobile@example.com", "9100000002");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"loginId\":\"9100000002\",\"password\":\"Str0ng!Pass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        registerAndVerify("login.wrong@example.com", "9100000003");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"loginId\":\"login.wrong@example.com\",\"password\":\"WrongPass1!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginForUnknownIdentifierIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"loginId\":\"nobody@example.com\",\"password\":\"WhateverPass1!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginLocksOutAfterRepeatedFailures() throws Exception {
        registerAndVerify("login.lockout@example.com", "9100000004");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{\"loginId\":\"login.lockout@example.com\",\"password\":\"WrongPass1!\"}"))
                    .andExpect(status().isUnauthorized());
        }

        // Even the correct password is now blocked until the lockout window (Redis-backed) clears.
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"loginId\":\"login.lockout@example.com\",\"password\":\"Str0ng!Pass1\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void loginOtpFlowEstablishesSession() throws Exception {
        registerAndVerify("login.otp@example.com", "9100000005");

        String responseBody = mockMvc.perform(post("/api/auth/login/otp")
                        .contentType("application/json")
                        .content("{\"loginId\":\"login.otp@example.com\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> otpResponse = objectMapper.readValue(responseBody, Map.class);
        String patientId = (String) otpResponse.get("patientId");
        String otp = otpDeliveryService.lastOtpFor("login.otp@example.com");

        mockMvc.perform(post("/api/auth/otp/verify")
                        .contentType("application/json")
                        .content("{\"patientId\":\"" + patientId + "\",\"purpose\":\"LOGIN\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void loginOtpForUnknownIdentifierIsNotFound() throws Exception {
        mockMvc.perform(post("/api/auth/login/otp")
                        .contentType("application/json")
                        .content("{\"loginId\":\"nobody@example.com\"}"))
                .andExpect(status().isNotFound());
    }
}
