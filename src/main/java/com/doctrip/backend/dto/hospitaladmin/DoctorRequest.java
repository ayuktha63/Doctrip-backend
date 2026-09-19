package com.doctrip.backend.dto.hospitaladmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DoctorRequest(
        @NotBlank String name,
        @NotNull UUID departmentId,
        String specialty,
        Integer experienceYears,
        String email) {}
