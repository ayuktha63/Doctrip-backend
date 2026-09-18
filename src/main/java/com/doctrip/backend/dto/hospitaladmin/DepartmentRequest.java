package com.doctrip.backend.dto.hospitaladmin;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(@NotBlank String name, @NotBlank String code, String head) {}
