package com.doctrip.backend.repository;

import com.doctrip.backend.entity.Hospital;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    Optional<Hospital> findByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    List<Hospital> findByActiveTrue();

    @Query(value = "select nextval('hospital_code_seq')", nativeQuery = true)
    long nextHospitalCodeSequence();
}
