package com.doctrip.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Issued once a patient becomes fully authenticated (FR-03/FR-04/FR-06/FR-07). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private PatientResponse patient;
}
