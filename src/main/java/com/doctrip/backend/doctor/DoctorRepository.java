package com.doctrip.backend.doctor;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByApprovedTrue();

    List<Doctor> findByApprovedTrueAndNameContainingIgnoreCase(String name);

    Optional<Doctor> findByIdAndApprovedTrue(Long id);

    List<Doctor> findByApprovedTrueAndHospitalIdAndIdNot(Long hospitalId, Long excludedId);

    List<Doctor> findByApprovedTrueAndHospitalId(Long hospitalId);

    List<Doctor> findByApprovedTrueAndHospitalIdAndDepartment(Long hospitalId, String department);
}
