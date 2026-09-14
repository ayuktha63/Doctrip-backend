package com.doctrip.backend.doctor;

public record DoctorSummaryDto(
        Long id,
        String name,
        String description) {

    public static DoctorSummaryDto from(Doctor doctor) {
        return new DoctorSummaryDto(
                doctor.getId(),
                doctor.getName(),
                doctor.getDescription());
    }
}
