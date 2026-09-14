package com.doctrip.backend.hospital;

public record HospitalSummaryDto(
        Long id,
        String name,
        String description) {

    public static HospitalSummaryDto from(Hospital hospital) {
        return new HospitalSummaryDto(
                hospital.getId(),
                hospital.getName(),
                hospital.getDescription());
    }
}
