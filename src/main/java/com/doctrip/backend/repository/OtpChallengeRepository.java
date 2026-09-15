package com.doctrip.backend.repository;

import com.doctrip.backend.entity.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, String> {

    Optional<OtpChallenge> findTopByPatientIdAndPurposeOrderByCreatedAtDesc(String patientId, String purpose);

    List<OtpChallenge> findByPatientIdAndPurposeAndConsumedFalseAndInvalidatedFalse(String patientId, String purpose);
}
