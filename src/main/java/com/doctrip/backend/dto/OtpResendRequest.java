package com.doctrip.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpResendRequest {

    @NotBlank(message = "Patient id is required.")
    private String patientId;

    @NotBlank(message = "Purpose is required.")
    private String purpose;
}
