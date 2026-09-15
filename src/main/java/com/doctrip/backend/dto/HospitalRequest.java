package com.doctrip.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HospitalRequest(
        @NotBlank String name,
        String type,
        @NotBlank String city,
        String state,
        String registrationNumber,
        String accreditation,
        @NotNull @Min(0) Integer beds,
        String contactPerson,
        @Email String contactEmail,
        String contactPhone) {}
