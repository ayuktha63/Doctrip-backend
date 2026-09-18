package com.doctrip.backend.dto.hospitaladmin;

import jakarta.validation.constraints.NotBlank;

public record HospitalAdminLoginRequest(@NotBlank String email, @NotBlank String password) {}
