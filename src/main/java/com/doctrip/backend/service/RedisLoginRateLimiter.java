package com.doctrip.backend.service;

import com.doctrip.backend.config.LoginProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Profile("!test")
public class RedisLoginRateLimiter implements LoginRateLimiter {

    private static final String KEY_PREFIX = "login:fail:";

    private final StringRedisTemplate redisTemplate;
    private final LoginProperties loginProperties;

    public RedisLoginRateLimiter(StringRedisTemplate redisTemplate, LoginProperties loginProperties) {
        this.redisTemplate = redisTemplate;
        this.loginProperties = loginProperties;
    }

    @Override
    public void recordFailure(String identifier) {
        String key = KEY_PREFIX + identifier;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(loginProperties.getLockoutWindowSeconds()));
        }
    }

    @Override
    public boolean isLocked(String identifier) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + identifier);
        if (value == null) {
            return false;
        }
        return Long.parseLong(value) >= loginProperties.getMaxFailedAttempts();
    }

    @Override
    public void reset(String identifier) {
        redisTemplate.delete(KEY_PREFIX + identifier);
    }
}
