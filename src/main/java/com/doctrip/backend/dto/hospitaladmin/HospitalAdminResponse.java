package com.doctrip.backend.dto.hospitaladmin;

import com.doctrip.backend.entity.HospitalAdmin;
import java.time.Instant;
import java.util.UUID;

public record HospitalAdminResponse(
        UUID id, UUID hospitalId, String email, String name, boolean active, Instant createdAt) {

    public static HospitalAdminResponse from(HospitalAdmin a) {
        return new HospitalAdminResponse(
                a.getId(), a.getHospitalId(), a.getEmail(), a.getName(), a.isActive(), a.getCreatedAt());
    }
}
