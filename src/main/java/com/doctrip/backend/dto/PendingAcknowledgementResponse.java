package com.doctrip.backend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * One row on the Hospital Admin "Doctor approvals" screen — a PENDING or ALTERNATIVE_PROPOSED
 * acknowledgement, flattened across every in-progress appointment request for the hospital (not
 * scoped to one request, unlike DoctorAcknowledgementSummary on AppointmentRequestDetailResponse).
 */
public record PendingAcknowledgementResponse(
        UUID ackId,
        UUID appointmentRequestId,
        String requestCode,
        String patientName,
        String department,
        String doctorName,
        String doctorSpecialty,
        String status,
        Instant sentAt,
        Instant responseDeadline,
        LocalDate proposedDate,
        LocalTime proposedTime) {}
