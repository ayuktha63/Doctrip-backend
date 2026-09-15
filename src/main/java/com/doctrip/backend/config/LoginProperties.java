package com.doctrip.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "doctrip.login")
@Getter
@Setter
public class LoginProperties {

    /** BR-06 — failed login attempts allowed within the lockout window before further attempts are blocked. */
    private int maxFailedAttempts = 5;

    private int lockoutWindowSeconds = 900;
}
