package com.doctrip.backend.doctor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByHospitalDoctorId(UUID hospitalDoctorId);

    /**
     * A doctor is publicly discoverable only when the doctor row itself is approved AND (it has no
     * hospital link at all, or its hospital is approved and not data-hidden) — otherwise a doctor
     * whose hospital was later deactivated or hidden would keep leaking through here even though
     * {@code DiscoveryHospitalService} correctly hides the hospital record itself.
     */
    @Query("select d from Doctor d where d.approved = true "
            + "and (d.hospital is null or (d.hospital.approved = true and d.hospital.dataHidden = false))")
    List<Doctor> findPubliclyDiscoverable();

    @Query("select d from Doctor d where d.approved = true "
            + "and lower(d.name) like lower(concat('%', :name, '%')) "
            + "and (d.hospital is null or (d.hospital.approved = true and d.hospital.dataHidden = false))")
    List<Doctor> findPubliclyDiscoverableByNameContaining(@Param("name") String name);

    @Query("select d from Doctor d where d.id = :id and d.approved = true "
            + "and (d.hospital is null or (d.hospital.approved = true and d.hospital.dataHidden = false))")
    Optional<Doctor> findPubliclyDiscoverableById(@Param("id") Long id);

    @Query("select d from Doctor d where d.approved = true and d.hospital.id = :hospitalId and d.id <> :excludedId "
            + "and d.hospital.approved = true and d.hospital.dataHidden = false")
    List<Doctor> findPubliclyDiscoverableByHospitalIdAndIdNot(
            @Param("hospitalId") Long hospitalId, @Param("excludedId") Long excludedId);

    @Query("select d from Doctor d where d.approved = true and d.hospital.id = :hospitalId "
            + "and d.hospital.approved = true and d.hospital.dataHidden = false")
    List<Doctor> findPubliclyDiscoverableByHospitalId(@Param("hospitalId") Long hospitalId);

    @Query("select d from Doctor d where d.approved = true and d.hospital.id = :hospitalId and d.department = :department "
            + "and d.hospital.approved = true and d.hospital.dataHidden = false")
    List<Doctor> findPubliclyDiscoverableByHospitalIdAndDepartment(
            @Param("hospitalId") Long hospitalId, @Param("department") String department);
}
