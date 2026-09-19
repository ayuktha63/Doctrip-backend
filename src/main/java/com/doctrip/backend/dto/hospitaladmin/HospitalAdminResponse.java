package com.doctrip.backend.dto.hospitaladmin;

import com.doctrip.backend.entity.HospitalAdmin;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code password} is the decrypted current password (see CredentialEncryptionService) — present
 * so DocTrip Admin's edit form can show it, not just accept a blind reset.
 */
public record HospitalAdminResponse(
        UUID id, UUID hospitalId, String email, String name, String password, boolean active, Instant createdAt) {

    public static HospitalAdminResponse from(HospitalAdmin a, String decryptedPassword) {
        return new HospitalAdminResponse(
                a.getId(), a.getHospitalId(), a.getEmail(), a.getName(), decryptedPassword, a.isActive(), a.getCreatedAt());
    }
}
