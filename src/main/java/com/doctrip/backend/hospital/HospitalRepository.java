package com.doctrip.backend.hospital;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    List<Hospital> findByApprovedTrue();

    List<Hospital> findByApprovedTrueAndNameContainingIgnoreCase(String name);

    Optional<Hospital> findByIdAndApprovedTrue(Long id);
}
