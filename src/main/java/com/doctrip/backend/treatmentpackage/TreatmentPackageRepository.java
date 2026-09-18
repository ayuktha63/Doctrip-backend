package com.doctrip.backend.treatmentpackage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentPackageRepository extends JpaRepository<TreatmentPackage, Long> {

    List<TreatmentPackage> findByApprovedTrue();

    Optional<TreatmentPackage> findByIdAndApprovedTrue(Long id);

    List<TreatmentPackage> findByHospital_Id(Long discoveryHospitalId);

    boolean existsByHospital_IdAndNameIgnoreCase(Long discoveryHospitalId, String name);

    boolean existsByHospital_IdAndNameIgnoreCaseAndIdNot(Long discoveryHospitalId, String name, Long id);
}
