package com.doctrip.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {

    @NotBlank(message = "Patient id is required.")
    private String patientId;

    /** REGISTER or LOGIN — must match the transaction the OTP was issued for. */
    @NotBlank(message = "Purpose is required.")
    private String purpose;

    @NotBlank(message = "Enter the 6-digit code we sent you.")
    @Pattern(regexp = "^[0-9]{6}$", message = "Enter the 6-digit code we sent you.")
    private String otp;
}
