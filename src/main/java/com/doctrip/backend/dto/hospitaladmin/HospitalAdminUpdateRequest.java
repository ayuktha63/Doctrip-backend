package com.doctrip.backend.dto.hospitaladmin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * {@code password} is optional — blank/absent leaves the current password unchanged, so DocTrip
 * Admin can edit just the name/email without being forced to reset a login every time.
 */
public record HospitalAdminUpdateRequest(
        @NotBlank String name, @NotBlank @Email String email, String password) {}
