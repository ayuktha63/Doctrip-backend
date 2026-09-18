package com.doctrip.backend.hospital;

import java.math.BigDecimal;

public record HospitalSummaryDto(
        Long id,
        String name,
        String description,
        String type,
        String city,
        Integer beds,
        BigDecimal rating,
        Integer reviewCount,
        String accreditation,
        String languages) {

    public static HospitalSummaryDto from(DiscoveryHospital hospital) {
        return new HospitalSummaryDto(
                hospital.getId(),
                hospital.getName(),
                hospital.getDescription(),
                hospital.getType(),
                hospital.getCity(),
                hospital.getBeds(),
                hospital.getRating(),
                hospital.getReviewCount(),
                hospital.getAccreditation(),
                hospital.getLanguages());
    }
}
