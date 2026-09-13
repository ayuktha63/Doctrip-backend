package com.doctrip.backend.treatmentpackage;

public record TreatmentPackageSummaryDto(
        Long id,
        String name,
        String description) {

    public static TreatmentPackageSummaryDto from(TreatmentPackage treatmentPackage) {
        return new TreatmentPackageSummaryDto(
                treatmentPackage.getId(),
                treatmentPackage.getName(),
                treatmentPackage.getDescription());
    }
}
