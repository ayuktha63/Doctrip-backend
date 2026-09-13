package com.doctrip.backend.treatmentpackage;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/treatment-packages")
public class TreatmentPackageController {

    private final TreatmentPackageService treatmentPackageService;

    public TreatmentPackageController(TreatmentPackageService treatmentPackageService) {
        this.treatmentPackageService = treatmentPackageService;
    }

    @GetMapping
    public List<TreatmentPackageSummaryDto> getTreatmentPackages() {
        return treatmentPackageService.getPubliclyDiscoverablePackages();
    }
}
