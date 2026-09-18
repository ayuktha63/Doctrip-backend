package com.doctrip.backend.dto.hospitaladmin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record HospitalAdminCreateRequest(
        @NotNull UUID hospitalId,
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotBlank String password) {}
