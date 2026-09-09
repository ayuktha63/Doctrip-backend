package com.doctrip.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Returned after FR-01 registration is accepted — OTP verification (FR-02/FR-03) is still pending. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String patientId;
    private String otpChannel;
    private String otpTarget;
    private int otpValiditySeconds;
}
