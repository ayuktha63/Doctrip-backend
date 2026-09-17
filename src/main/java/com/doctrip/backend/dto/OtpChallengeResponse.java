package com.doctrip.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Returned whenever an OTP has just been issued (registration FR-01/FR-02, or login FR-05) and is pending verification. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpChallengeResponse {
    private String patientId;
    private String otpChannel;
    private String otpTarget;
    private int otpValiditySeconds;
}
