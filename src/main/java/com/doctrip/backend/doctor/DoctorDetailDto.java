package com.doctrip.backend.doctor;

import java.math.BigDecimal;
import java.util.List;

public record DoctorDetailDto(
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
        String hospitalName,
        List<DoctorTeamMemberDto> team) {

    public static DoctorDetailDto from(Doctor doctor, List<Doctor> colleagues) {
        return new DoctorDetailDto(
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
                doctor.getHospital() != null ? doctor.getHospital().getName() : null,
                colleagues.stream().map(DoctorTeamMemberDto::from).toList());
    }
}
