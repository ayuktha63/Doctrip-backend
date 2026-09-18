package com.doctrip.backend.repository;

import com.doctrip.backend.entity.HospitalDoctor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalDoctorRepository extends JpaRepository<HospitalDoctor, UUID> {

    List<HospitalDoctor> findByHospitalId(UUID hospitalId);

    long countByDepartmentId(UUID departmentId);

    boolean existsByHospitalIdAndNameIgnoreCase(UUID hospitalId, String name);

    boolean existsByHospitalIdAndNameIgnoreCaseAndIdNot(UUID hospitalId, String name, UUID id);
}
