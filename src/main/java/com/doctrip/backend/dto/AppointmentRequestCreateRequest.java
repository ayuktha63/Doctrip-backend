package com.doctrip.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * The discover -> booking flow's submit step (Doctrip-frontend's BookingRequestStateService).
 * {@code discoveryHospitalId} is the Story 2 public-discovery hospital id (what the booking
 * screen actually has from the treatment/hospital/doctor record it started from) — the service
 * resolves it to the real admin {@code entity.Hospital} via {@code DiscoveryHospital.adminHospitalId}
 * so this request routes into Hospital Admin's real coordination queue.
 */
public record AppointmentRequestCreateRequest(
        @NotNull Long discoveryHospitalId,
        @NotBlank String department,
        String requestedDoctorName,
        @NotNull LocalDate requestedDate,
        @NotNull LocalTime requestedTime,
        @NotBlank String healthcareRequirement,
        String additionalInfo,
        String patientCountry,
        Integer patientAge,
        List<String> requiredDocuments) {}
