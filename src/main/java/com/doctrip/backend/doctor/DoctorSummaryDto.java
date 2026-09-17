package com.doctrip.backend.doctor;

import java.math.BigDecimal;

public record DoctorSummaryDto(
        Long id,
        String name,
        String description,
        String department,
        String specialty,
        Integer experienceYears,
        BigDecimal rating,
        Integer reviewCount,
        String languages,
        Long hospitalId,
        String hospitalName) {

    public static DoctorSummaryDto from(Doctor doctor) {
        return new DoctorSummaryDto(
                doctor.getId(),
                doctor.getName(),
                doctor.getDescription(),
                doctor.getDepartment(),
                doctor.getSpecialty(),
                doctor.getExperienceYears(),
                doctor.getRating(),
                doctor.getReviewCount(),
                doctor.getLanguages(),
                doctor.getHospital() != null ? doctor.getHospital().getId() : null,
                doctor.getHospital() != null ? doctor.getHospital().getName() : null);
    }
}
