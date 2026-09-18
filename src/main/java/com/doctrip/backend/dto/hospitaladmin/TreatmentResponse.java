package com.doctrip.backend.dto.hospitaladmin;

import com.doctrip.backend.treatmentpackage.TreatmentPackage;
import java.math.BigDecimal;
import java.util.UUID;

public record TreatmentResponse(
        Long id,
        String name,
        UUID departmentId,
        String departmentName,
        BigDecimal price,
        BigDecimal consultationFee,
        String typicalStay,
        String description,
        boolean listed) {

    public static TreatmentResponse from(TreatmentPackage t, UUID departmentId) {
        return new TreatmentResponse(
                t.getId(), t.getName(), departmentId, t.getDepartment(), t.getPrice(), t.getConsultationFee(),
                t.getTypicalStay(), t.getDescription(), t.isApproved());
    }
}
