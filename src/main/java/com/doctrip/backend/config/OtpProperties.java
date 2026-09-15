package com.doctrip.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "doctrip.otp")
@Getter
@Setter
public class OtpProperties {

    /** BR-03 — how long an issued OTP stays valid. */
    private int validitySeconds = 300;

    /** BR-06/AC-09 — verification attempts allowed before the OTP is locked out. */
    private int maxAttempts = 5;

    /** BR-06/AC-08 — minimum gap between two OTP issuances for the same patient+purpose. */
    private int resendCooldownSeconds = 30;

    private int length = 6;
}
