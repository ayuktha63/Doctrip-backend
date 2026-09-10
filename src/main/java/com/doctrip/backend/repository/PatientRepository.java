package com.doctrip.backend.repository;

import com.doctrip.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, String> {
    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByMobile(String mobile);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    default Optional<Patient> findByEmailOrMobile(String loginId) {
        return findByEmail(loginId).or(() -> findByMobile(loginId));
    }
}
