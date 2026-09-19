package com.doctrip.backend.treatmentpackage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TreatmentPackageRepository extends JpaRepository<TreatmentPackage, Long> {

    List<TreatmentPackage> findByApprovedTrue();

    Optional<TreatmentPackage> findByIdAndApprovedTrue(Long id);

    List<TreatmentPackage> findByHospital_Id(Long discoveryHospitalId);

    boolean existsByHospital_IdAndNameIgnoreCase(Long discoveryHospitalId, String name);

    boolean existsByHospital_IdAndNameIgnoreCaseAndIdNot(Long discoveryHospitalId, String name, Long id);

    List<TreatmentPackage> findByApprovedTrueAndDepartment(String department);

    @Query("select distinct t.department from TreatmentPackage t where t.approved = true and t.department is not null")
    List<String> findDistinctDepartments();
}
