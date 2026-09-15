package com.doctrip.backend.service;

import com.doctrip.backend.config.LoginProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Test-only stand-in for RedisLoginRateLimiter — same counter semantics, no Redis required. */
@Service
@Profile("test")
public class InMemoryLoginRateLimiter implements LoginRateLimiter {

    private final Map<String, AtomicInteger> failures = new ConcurrentHashMap<>();
    private final LoginProperties loginProperties;

    public InMemoryLoginRateLimiter(LoginProperties loginProperties) {
        this.loginProperties = loginProperties;
    }

    @Override
    public void recordFailure(String identifier) {
        failures.computeIfAbsent(identifier, k -> new AtomicInteger()).incrementAndGet();
    }

    @Override
    public boolean isLocked(String identifier) {
        AtomicInteger count = failures.get(identifier);
        return count != null && count.get() >= loginProperties.getMaxFailedAttempts();
    }

    @Override
    public void reset(String identifier) {
        failures.remove(identifier);
    }
}
