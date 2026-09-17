package com.doctrip.backend.treatmentpackage;

import com.doctrip.backend.doctor.Doctor;
import com.doctrip.backend.doctor.DoctorTeamMemberDto;
import java.math.BigDecimal;
import java.util.List;

public record TreatmentPackageDetailDto(
        Long id,
        String name,
        String description,
        String department,
        BigDecimal price,
        String typicalStay,
        Long hospitalId,
        String hospitalName,
        String hospitalCity,
        String hospitalAccreditation,
        List<DoctorTeamMemberDto> team) {

    public static TreatmentPackageDetailDto from(TreatmentPackage treatmentPackage, List<Doctor> team) {
        return new TreatmentPackageDetailDto(
                treatmentPackage.getId(),
                treatmentPackage.getName(),
                treatmentPackage.getDescription(),
                treatmentPackage.getDepartment(),
                treatmentPackage.getPrice(),
                treatmentPackage.getTypicalStay(),
                treatmentPackage.getHospital() != null ? treatmentPackage.getHospital().getId() : null,
                treatmentPackage.getHospital() != null ? treatmentPackage.getHospital().getName() : null,
                treatmentPackage.getHospital() != null ? treatmentPackage.getHospital().getCity() : null,
                treatmentPackage.getHospital() != null ? treatmentPackage.getHospital().getAccreditation() : null,
                team.stream().map(DoctorTeamMemberDto::from).toList());
    }
}
