package com.doctrip.backend.doctor;

public record DoctorTeamMemberDto(
        Long id,
        String name,
        String specialty,
        String department,
        Integer experienceYears) {

    public static DoctorTeamMemberDto from(Doctor doctor) {
        return new DoctorTeamMemberDto(
                doctor.getId(),
                doctor.getName(),
                doctor.getSpecialty(),
                doctor.getDepartment(),
                doctor.getExperienceYears());
    }
}
