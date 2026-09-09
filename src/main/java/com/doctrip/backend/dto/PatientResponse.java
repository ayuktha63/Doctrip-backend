package com.doctrip.backend.dto;

import com.doctrip.backend.entity.Patient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {
    private String id;
    private String fullName;
    private String email;
    private String mobile;
    private boolean emailVerified;
    private boolean mobileVerified;
    private String status;

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFullName(),
                patient.getEmail(),
                patient.getMobile(),
                patient.isEmailVerified(),
                patient.isMobileVerified(),
                patient.getStatus()
        );
    }
}
