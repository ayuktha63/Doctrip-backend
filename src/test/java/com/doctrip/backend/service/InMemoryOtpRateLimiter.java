package com.doctrip.backend.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Test-only stand-in for RedisOtpRateLimiter — same NX+TTL semantics, no Redis required. */
@Service
@Profile("test")
public class InMemoryOtpRateLimiter implements OtpRateLimiter {

    private final Map<String, Instant> cooldownUntil = new ConcurrentHashMap<>();

    @Override
    public synchronized boolean tryAcquire(String key, int cooldownSeconds) {
        Instant now = Instant.now();
        Instant until = cooldownUntil.get(key);
        if (until != null && until.isAfter(now)) {
            return false;
        }
        cooldownUntil.put(key, now.plusSeconds(cooldownSeconds));
        return true;
    }
}
