package com.doctrip.backend.hospital;

import com.doctrip.backend.doctor.Doctor;
import com.doctrip.backend.doctor.DoctorTeamMemberDto;
import java.math.BigDecimal;
import java.util.List;

public record HospitalDetailDto(
        Long id,
        String name,
        String description,
        String type,
        String city,
        Integer beds,
        BigDecimal rating,
        Integer reviewCount,
        String accreditation,
        String languages,
        Integer departmentCount,
        List<DoctorTeamMemberDto> team) {

    public static HospitalDetailDto from(DiscoveryHospital hospital, List<Doctor> doctors) {
        int departmentCount = (int) doctors.stream()
                .map(Doctor::getDepartment)
                .filter(department -> department != null)
                .distinct()
                .count();
        return new HospitalDetailDto(
                hospital.getId(),
                hospital.getName(),
                hospital.getDescription(),
                hospital.getType(),
                hospital.getCity(),
                hospital.getBeds(),
                hospital.getRating(),
                hospital.getReviewCount(),
                hospital.getAccreditation(),
                hospital.getLanguages(),
                departmentCount,
                doctors.stream().map(DoctorTeamMemberDto::from).toList());
    }
}
