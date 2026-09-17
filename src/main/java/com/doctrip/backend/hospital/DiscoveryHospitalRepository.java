package com.doctrip.backend.hospital;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoveryHospitalRepository extends JpaRepository<DiscoveryHospital, Long> {

    List<DiscoveryHospital> findByApprovedTrue();

    List<DiscoveryHospital> findByApprovedTrueAndNameContainingIgnoreCase(String name);

    Optional<DiscoveryHospital> findByIdAndApprovedTrue(Long id);
}
