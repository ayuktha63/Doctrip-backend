package com.doctrip.backend.treatmentpackage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentPackageRepository extends JpaRepository<TreatmentPackage, Long> {

    List<TreatmentPackage> findByApprovedTrue();
}
