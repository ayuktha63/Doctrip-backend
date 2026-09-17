package com.doctrip.backend.service;

import com.doctrip.backend.config.OtpProperties;
import com.doctrip.backend.entity.OtpChallenge;
import com.doctrip.backend.exception.BadRequestException;
import com.doctrip.backend.exception.TooManyRequestsException;
import com.doctrip.backend.repository.OtpChallengeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Generates, delivers, and verifies OTP challenges (FR-02/FR-05). The plaintext OTP is handed to
 * the caller only long enough to pass to {@link OtpDeliveryService} — never persisted, logged, or
 * returned from this class otherwise (BR-06 / audit trail requirement).
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpChallengeRepository otpChallengeRepository;
    private final OtpDeliveryService otpDeliveryService;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProperties;
    private final AuditService auditService;
    private final OtpAttemptRecorder otpAttemptRecorder;
    private final OtpRateLimiter otpRateLimiter;

    public OtpService(OtpChallengeRepository otpChallengeRepository,
                       OtpDeliveryService otpDeliveryService,
                       PasswordEncoder passwordEncoder,
                       OtpProperties otpProperties,
                       AuditService auditService,
                       OtpAttemptRecorder otpAttemptRecorder,
                       OtpRateLimiter otpRateLimiter) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.otpDeliveryService = otpDeliveryService;
        this.passwordEncoder = passwordEncoder;
        this.otpProperties = otpProperties;
        this.auditService = auditService;
        this.otpAttemptRecorder = otpAttemptRecorder;
        this.otpRateLimiter = otpRateLimiter;
    }

    @Transactional
    public OtpChallenge issue(String patientId, String purpose, String channel, String target) {
        // BR-06/AC-08 — Redis-backed cooldown (SET NX EX), not a DB timestamp comparison: atomic
        // across concurrent requests and doesn't need its own DB row to derive state from.
        if (!otpRateLimiter.tryAcquire(patientId + ":" + purpose, otpProperties.getResendCooldownSeconds())) {
            throw new TooManyRequestsException("Please wait before requesting another code.");
        }

        // BR-07: a newly issued OTP invalidates any previously issued, still-live OTP for the same transaction.
        List<OtpChallenge> live = otpChallengeRepository
                .findByPatientIdAndPurposeAndConsumedFalseAndInvalidatedFalse(patientId, purpose);
        live.forEach(o -> o.setInvalidated(true));
        otpChallengeRepository.saveAll(live);

        String otp = generateCode();
        OtpChallenge challenge = OtpChallenge.builder()
                .patientId(patientId)
                .purpose(purpose)
                .channel(channel)
                .target(target)
                .codeHash(passwordEncoder.encode(otp))
                .expiresAt(Instant.now().plus(otpProperties.getValiditySeconds(), ChronoUnit.SECONDS))
                .maxAttempts(otpProperties.getMaxAttempts())
                .build();
        otpChallengeRepository.save(challenge);

        otpDeliveryService.send(channel, target, otp);
        auditService.record(patientId, "otp_requested", channel);
        return challenge;
    }

    @Transactional
    public void verify(String patientId, String purpose, String otp) {
        OtpChallenge challenge = otpChallengeRepository
                .findTopByPatientIdAndPurposeOrderByCreatedAtDesc(patientId, purpose)
                .orElseThrow(() -> new BadRequestException("No code was requested for this transaction."));

        if (challenge.isConsumed()) {
            throw new BadRequestException("This code has already been used. Request a new one.");
        }
        if (challenge.isInvalidated()) {
            throw new BadRequestException("This code is no longer valid. Request a new one.");
        }
        if (challenge.isExpired()) {
            auditService.record(patientId, "otp_expired", challenge.getChannel());
            throw new BadRequestException("This code has expired. Request a new one.");
        }
        if (challenge.getAttempts() >= challenge.getMaxAttempts()) {
            auditService.record(patientId, "otp_locked", challenge.getChannel());
            throw new TooManyRequestsException("Too many incorrect attempts. Request a new code.");
        }

        challenge.setAttempts(challenge.getAttempts() + 1);
        if (!passwordEncoder.matches(otp, challenge.getCodeHash())) {
            otpAttemptRecorder.recordAttempt(challenge);
            auditService.record(patientId, "otp_verify_failed", challenge.getChannel());
            throw new BadRequestException("Enter the 6-digit code we sent you.");
        }

        challenge.setConsumed(true);
        otpChallengeRepository.save(challenge);
        auditService.record(patientId, "otp_verify_succeeded", challenge.getChannel());
    }

    private String generateCode() {
        int max = (int) Math.pow(10, otpProperties.getLength());
        int value = RANDOM.nextInt(max);
        return String.format("%0" + otpProperties.getLength() + "d", value);
    }
}
