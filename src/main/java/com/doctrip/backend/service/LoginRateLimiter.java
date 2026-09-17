package com.doctrip.backend.service;

/** BR-06 — throttles repeated failed login attempts per identity (email/mobile). */
public interface LoginRateLimiter {

    void recordFailure(String identifier);

    boolean isLocked(String identifier);

    void reset(String identifier);
}
