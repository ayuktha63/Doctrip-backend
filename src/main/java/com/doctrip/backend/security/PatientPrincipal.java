package com.doctrip.backend.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Minimal authenticated-principal wrapper carrying just the patient id. */
@Getter
@AllArgsConstructor
public class PatientPrincipal {
    private final String patientId;
}
