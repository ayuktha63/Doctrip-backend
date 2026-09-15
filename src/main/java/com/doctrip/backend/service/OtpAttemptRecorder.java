package com.doctrip.backend.service;

import com.doctrip.backend.entity.OtpChallenge;
import com.doctrip.backend.repository.OtpChallengeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists a failed OTP attempt in its own transaction, independent of the caller's.
 * OtpService.verify() throws a (rollback-triggering) RuntimeException on a wrong/expired/locked
 * OTP, which would otherwise undo the attempt-count increment along with everything else in the
 * same transaction — silently defeating the attempt limit (BR-06/AC-09).
 */
@Service
public class OtpAttemptRecorder {

    private final OtpChallengeRepository otpChallengeRepository;

    public OtpAttemptRecorder(OtpChallengeRepository otpChallengeRepository) {
        this.otpChallengeRepository = otpChallengeRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(OtpChallenge challenge) {
        otpChallengeRepository.save(challenge);
    }
}
