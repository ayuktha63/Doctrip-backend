package com.doctrip.backend.dto.hospitaladmin;

import com.doctrip.backend.entity.DoctorAvailability;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityResponse(
        UUID id, UUID doctorId, LocalDate slotDate, LocalTime startTime, LocalTime endTime, String kind) {

    public static AvailabilityResponse from(DoctorAvailability a) {
        return new AvailabilityResponse(
                a.getId(), a.getDoctorId(), a.getSlotDate(), a.getStartTime(), a.getEndTime(), a.getKind());
    }
}
