package com.doctrip.backend.treatmentpackage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentPackageRepository extends JpaRepository<TreatmentPackage, Long> {

    List<TreatmentPackage> findByApprovedTrue();

    Optional<TreatmentPackage> findByIdAndApprovedTrue(Long id);
}
