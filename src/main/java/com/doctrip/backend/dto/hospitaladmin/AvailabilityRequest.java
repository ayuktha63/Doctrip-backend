package com.doctrip.backend.dto.hospitaladmin;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityRequest(
        @NotNull LocalDate slotDate, @NotNull LocalTime startTime, @NotNull LocalTime endTime, String kind) {}
