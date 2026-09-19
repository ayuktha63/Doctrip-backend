package com.doctrip.backend.dto.hospitaladmin;

import com.doctrip.backend.entity.HospitalDoctor;
import java.util.UUID;

public record DoctorResponse(
        UUID id,
        String name,
        UUID departmentId,
        String departmentName,
        String specialty,
        Integer experienceYears,
        String email,
        boolean active) {

    public static DoctorResponse from(HospitalDoctor d, String departmentName) {
        return new DoctorResponse(
                d.getId(), d.getName(), d.getDepartmentId(), departmentName, d.getSpecialty(),
                d.getExperienceYears(), d.getEmail(), d.isActive());
    }
}
