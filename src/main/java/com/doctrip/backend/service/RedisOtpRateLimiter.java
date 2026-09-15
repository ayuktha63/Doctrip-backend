package com.doctrip.backend.service;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** SET NX EX — atomic "claim this cooldown window or tell me it's already running" in one round trip. */
@Service
@Profile("!test")
public class RedisOtpRateLimiter implements OtpRateLimiter {

    private static final String KEY_PREFIX = "otp:cooldown:";

    private final StringRedisTemplate redisTemplate;

    public RedisOtpRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int cooldownSeconds) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + key, "1", Duration.ofSeconds(cooldownSeconds));
        return Boolean.TRUE.equals(acquired);
    }
}
