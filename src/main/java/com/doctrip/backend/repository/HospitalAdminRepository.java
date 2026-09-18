package com.doctrip.backend.repository;

import com.doctrip.backend.entity.HospitalAdmin;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalAdminRepository extends JpaRepository<HospitalAdmin, UUID> {

    Optional<HospitalAdmin> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<HospitalAdmin> findByHospitalId(UUID hospitalId);
}
