package com.doctrip.backend.dto;

import com.doctrip.backend.entity.Patient;
import java.time.Instant;
import java.util.List;

/**
 * DocTrip Admin's "Users" list — every registered patient, regardless of which client they signed
 * up through (the Angular web frontend and the Flutter mobile app both authenticate against this
 * same Story 1 API, so there is only ever one patient list to show, not a separate one per
 * platform).
 */
public record AdminPatientResponse(
        String id,
        String fullName,
        String email,
        String mobile,
        boolean emailVerified,
        boolean mobileVerified,
        String status,
        List<String> authProviders,
        Instant createdAt) {

    public static AdminPatientResponse from(Patient patient) {
        return new AdminPatientResponse(
                patient.getId(),
                patient.getFullName(),
                patient.getEmail(),
                patient.getMobile(),
                patient.isEmailVerified(),
                patient.isMobileVerified(),
                patient.getStatus(),
                patient.getAuthProviders(),
                patient.getCreatedAt());
    }
}
