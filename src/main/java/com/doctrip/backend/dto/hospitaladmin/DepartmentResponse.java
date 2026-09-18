package com.doctrip.backend.dto.hospitaladmin;

import com.doctrip.backend.entity.Department;
import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String name,
        String code,
        String head,
        boolean active,
        long doctorCount,
        Instant createdAt,
        Instant updatedAt) {

    public static DepartmentResponse from(Department d, long doctorCount) {
        return new DepartmentResponse(
                d.getId(), d.getName(), d.getCode(), d.getHead(), d.isActive(), doctorCount,
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
