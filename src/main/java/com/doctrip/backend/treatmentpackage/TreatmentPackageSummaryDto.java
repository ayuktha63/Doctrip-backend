package com.doctrip.backend.treatmentpackage;

import java.math.BigDecimal;

public record TreatmentPackageSummaryDto(
        Long id,
        String name,
        String description,
        String department,
        BigDecimal price,
        String typicalStay,
        Long hospitalId,
        String hospitalName) {

    public static TreatmentPackageSummaryDto from(TreatmentPackage treatmentPackage) {
        return new TreatmentPackageSummaryDto(
                treatmentPackage.getId(),
                treatmentPackage.getName(),
                treatmentPackage.getDescription(),
                treatmentPackage.getDepartment(),
                treatmentPackage.getPrice(),
                treatmentPackage.getTypicalStay(),
                treatmentPackage.getHospital() != null ? treatmentPackage.getHospital().getId() : null,
                treatmentPackage.getHospital() != null ? treatmentPackage.getHospital().getName() : null);
    }
}
