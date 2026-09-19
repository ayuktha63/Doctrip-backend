package com.doctrip.backend.hospital;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoveryHospitalRepository extends JpaRepository<DiscoveryHospital, Long> {

    List<DiscoveryHospital> findByApprovedTrueAndDataHiddenFalse();

    List<DiscoveryHospital> findByApprovedTrueAndDataHiddenFalseAndNameContainingIgnoreCase(String name);

    Optional<DiscoveryHospital> findByIdAndApprovedTrueAndDataHiddenFalse(Long id);

    Optional<DiscoveryHospital> findByAdminHospitalId(UUID adminHospitalId);
}
