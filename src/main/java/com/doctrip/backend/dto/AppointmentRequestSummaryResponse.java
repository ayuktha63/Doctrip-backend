package com.doctrip.backend.dto;

import com.doctrip.backend.entity.AppointmentRequestStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Row shape for the Hospital Admin "Appointment requests" list (FR-01) — reused for the
 * Patient-facing "My appointments" list too, which is why {@code hospitalName} is here even
 * though Hospital Admin already knows its own hospital's name.
 *
 * <p>{@code secured*} fields are only populated once a doctor/slot is actually secured
 * (DOCTOR_SELECTED/CONFIRMED) — via the originally requested doctor accepting, or via Hospital
 * Admin resolving a doctor-proposed alternative (FR-08). Without these, "My appointments" kept
 * showing the patient's original requested date/time forever, even after a different time was
 * proposed and accepted — the one place that actually reflected the change was the detail screen.
 */
public record AppointmentRequestSummaryResponse(
        UUID id,
        String requestCode,
        String hospitalName,
        String patientName,
        String patientCountry,
        String department,
        String requestedDoctorName,
        LocalDate requestedDate,
        LocalTime requestedTime,
        AppointmentRequestStatus status,
        Instant adminSlaDeadline,
        boolean adminSlaBreached,
        String securedDoctorName,
        LocalDate securedDate,
        LocalTime securedTime) {}
