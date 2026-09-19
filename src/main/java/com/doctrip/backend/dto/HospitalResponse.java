package com.doctrip.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record HospitalResponse(
        UUID id,
        String hospitalCode,
        String name,
        String type,
        String city,
        String state,
        String registrationNumber,
        String accreditation,
        int beds,
        String contactPerson,
        String contactEmail,
        String contactPhone,
        boolean active,
        boolean dataHidden,
        Instant createdAt,
        Instant updatedAt) {}
