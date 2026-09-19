package com.doctrip.backend.repository;

import com.doctrip.backend.entity.CoordinationDoctor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoordinationDoctorRepository extends JpaRepository<CoordinationDoctor, UUID> {

    List<CoordinationDoctor> findByHospitalIdAndDepartmentAndAvailableTrue(UUID hospitalId, String department);

    Optional<CoordinationDoctor> findByHospitalDoctorId(UUID hospitalDoctorId);
}
