package com.doctrip.backend.treatmentpackage;

import java.util.List;

import org.springframework.stereotype.Service;

import com.doctrip.backend.common.ResourceNotFoundException;

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

    public TreatmentPackageSummaryDto getPubliclyDiscoverablePackageById(Long id) {
        return treatmentPackageRepository.findByIdAndApprovedTrue(id)
                .map(TreatmentPackageSummaryDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment package not found"));
    }
}
