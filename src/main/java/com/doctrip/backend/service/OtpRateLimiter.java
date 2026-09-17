package com.doctrip.backend.service;

/** BR-06/AC-08 — throttles OTP resend/issuance per patient+purpose. */
public interface OtpRateLimiter {

    /**
     * Attempts to acquire the right to issue a new OTP for this key. Returns true and starts the
     * cooldown window if none is currently active; returns false if a cooldown from a previous
     * issuance is still running.
     */
    boolean tryAcquire(String key, int cooldownSeconds);
}
