package com.doctrip.backend.hospital;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    List<Hospital> findByApprovedTrue();
}
