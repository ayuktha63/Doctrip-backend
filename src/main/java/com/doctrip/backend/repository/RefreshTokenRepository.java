package com.doctrip.backend.repository;

import com.doctrip.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByPatientIdAndRevokedFalse(String patientId);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.patientId = :patientId and r.revoked = false")
    void revokeAllForPatient(String patientId);
}
