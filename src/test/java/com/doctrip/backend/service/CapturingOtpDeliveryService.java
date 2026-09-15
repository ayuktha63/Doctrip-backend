package com.doctrip.backend.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/** Test-only OTP "delivery" that captures the last OTP sent per target so tests can complete the flow. */
@Service
@Profile("test")
public class CapturingOtpDeliveryService implements OtpDeliveryService {

    private final Map<String, String> lastOtpByTarget = new ConcurrentHashMap<>();

    @Override
    public void send(String channel, String target, String otp) {
        lastOtpByTarget.put(target, otp);
    }

    public String lastOtpFor(String target) {
        return lastOtpByTarget.get(target);
    }
}
