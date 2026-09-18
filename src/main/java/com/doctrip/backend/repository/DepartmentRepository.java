package com.doctrip.backend.repository;

import com.doctrip.backend.entity.Department;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findByHospitalId(UUID hospitalId);

    boolean existsByHospitalIdAndCodeIgnoreCase(UUID hospitalId, String code);

    boolean existsByHospitalIdAndCodeIgnoreCaseAndIdNot(UUID hospitalId, String code, UUID id);
}
