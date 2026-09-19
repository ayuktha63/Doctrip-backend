package com.doctrip.backend.treatmentpackage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TreatmentPackageRepository extends JpaRepository<TreatmentPackage, Long> {

    /**
     * Publicly discoverable only when the package itself is approved AND (it has no hospital link,
     * or its hospital is approved and not data-hidden) — same reasoning as
     * {@code DoctorRepository.findPubliclyDiscoverable}.
     */
    @Query("select t from TreatmentPackage t where t.approved = true "
            + "and (t.hospital is null or (t.hospital.approved = true and t.hospital.dataHidden = false))")
    List<TreatmentPackage> findPubliclyDiscoverable();

    @Query("select t from TreatmentPackage t where t.id = :id and t.approved = true "
            + "and (t.hospital is null or (t.hospital.approved = true and t.hospital.dataHidden = false))")
    Optional<TreatmentPackage> findPubliclyDiscoverableById(@Param("id") Long id);

    List<TreatmentPackage> findByHospital_Id(Long discoveryHospitalId);

    boolean existsByHospital_IdAndNameIgnoreCase(Long discoveryHospitalId, String name);

    boolean existsByHospital_IdAndNameIgnoreCaseAndIdNot(Long discoveryHospitalId, String name, Long id);
}
