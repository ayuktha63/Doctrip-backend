package com.doctrip.backend.doctor;

import com.doctrip.backend.entity.DoctorAvailability;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** A doctor's real, hospital-admin-published open slot, as patients see it during booking. */
public record DoctorAvailabilitySlotDto(UUID id, LocalDate slotDate, LocalTime startTime, LocalTime endTime, String kind) {

    public static DoctorAvailabilitySlotDto from(DoctorAvailability a) {
        return new DoctorAvailabilitySlotDto(a.getId(), a.getSlotDate(), a.getStartTime(), a.getEndTime(), a.getKind());
    }
}
