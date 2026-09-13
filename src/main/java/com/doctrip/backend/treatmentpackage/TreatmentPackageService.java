package com.doctrip.backend.treatmentpackage;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TreatmentPackageService {

    private final TreatmentPackageRepository treatmentPackageRepository;

    public TreatmentPackageService(TreatmentPackageRepository treatmentPackageRepository) {
        this.treatmentPackageRepository = treatmentPackageRepository;
    }

    public List<TreatmentPackageSummaryDto> getPubliclyDiscoverablePackages() {
        return treatmentPackageRepository.findByApprovedTrue()
                .stream()
                .map(TreatmentPackageSummaryDto::from)
                .toList();
    }
}
