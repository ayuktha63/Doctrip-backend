package com.doctrip.backend.dto;

import com.doctrip.backend.entity.AppointmentRequestStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Row shape for the Hospital Admin "Appointment requests" list (FR-01). */
public record AppointmentRequestSummaryResponse(
        UUID id,
        String requestCode,
        String patientName,
        String patientCountry,
        String department,
        String requestedDoctorName,
        LocalDate requestedDate,
        LocalTime requestedTime,
        AppointmentRequestStatus status,
        Instant adminSlaDeadline,
        boolean adminSlaBreached) {}
