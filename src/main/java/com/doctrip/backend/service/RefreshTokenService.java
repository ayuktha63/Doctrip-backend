package com.doctrip.backend.service;

import com.doctrip.backend.entity.RefreshToken;
import com.doctrip.backend.exception.UnauthorizedException;
import com.doctrip.backend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * FR-08 — issues, verifies, and rotates refresh tokens. A refresh token is a high-entropy opaque
 * random value (not a JWT) so it can be revoked server-side, unlike the stateless access token.
 * Rotated on every use: the presented token is revoked and a new one issued in the same call, so a
 * stolen-and-reused refresh token is detectable (its use after rotation will fail — see verify()).
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirySeconds;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                                @Value("${doctrip.jwt.refresh-expiry-seconds:2592000}") long refreshExpirySeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirySeconds = refreshExpirySeconds;
    }

    @Transactional
    public String issue(String patientId) {
        String rawToken = generateRawToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .patientId(patientId)
                .tokenHash(TokenHasher.sha256Hex(rawToken))
                .expiresAt(Instant.now().plusSeconds(refreshExpirySeconds))
                .build());
        return rawToken;
    }

    /** Verifies the presented refresh token, revokes it, and issues its replacement (rotation). Returns {patientId, newRawToken}. */
    @Transactional
    public Rotated verifyAndRotate(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid session. Please log in again."));
        if (!token.isUsable()) {
            throw new UnauthorizedException("Your session has expired. Please log in again.");
        }
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        String newToken = issue(token.getPatientId());
        return new Rotated(token.getPatientId(), newToken);
    }

    /** FR-09 — invalidates every refresh token for a patient (used on logout). */
    @Transactional
    public void revokeAllForPatient(String patientId) {
        refreshTokenRepository.revokeAllForPatient(patientId);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record Rotated(String patientId, String newRawToken) {
    }
}
