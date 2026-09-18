package com.doctrip.backend.dto.hospitaladmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record TreatmentRequest(
        @NotBlank String name,
        @NotNull UUID departmentId,
        BigDecimal price,
        BigDecimal consultationFee,
        String typicalStay,
        String description) {}
