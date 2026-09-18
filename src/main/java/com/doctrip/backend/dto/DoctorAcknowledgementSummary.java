package com.doctrip.backend.dto;

import com.doctrip.backend.entity.DoctorAcknowledgementStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DoctorAcknowledgementSummary(
        UUID id,
        UUID doctorId,
        String doctorName,
        String doctorSpecialty,
        DoctorAcknowledgementStatus status,
        Instant sentAt,
        Instant responseDeadline,
        LocalDate proposedDate,
        LocalTime proposedTime) {}
