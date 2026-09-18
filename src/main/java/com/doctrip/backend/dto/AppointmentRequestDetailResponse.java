package com.doctrip.backend.dto;

import com.doctrip.backend.entity.AppointmentRequestStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Full detail shape for Hospital Admin reviewing one appointment request (FR-01). */
public record AppointmentRequestDetailResponse(
        UUID id,
        String requestCode,
        String patientId,
        String patientName,
        String patientCountry,
        Integer patientAge,
        String patientGender,
        UUID hospitalId,
        String hospitalName,
        String department,
        String requestedDoctorName,
        LocalDate requestedDate,
        LocalTime requestedTime,
        String healthcareRequirement,
        String additionalInfo,
        List<String> requiredDocuments,
        AppointmentRequestStatus status,
        Instant adminSlaDeadline,
        boolean adminSlaBreached,
        Instant createdAt,
        Instant updatedAt,
        List<DoctorAcknowledgementSummary> coordination,
        String securedDoctorName,
        LocalDate securedDate,
        LocalTime securedTime) {}
