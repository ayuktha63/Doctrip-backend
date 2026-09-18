package com.doctrip.backend.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Was hardcoded to :4300 only, independent of doctrip.cors.allowed-origin (used everywhere
    // else, including SecurityConfig's own comma-separated multi-origin support for the Flutter
    // web build) — meant a single-port frontend could never satisfy both this and SecurityConfig's
    // CORS rules at once. Now shares the same configurable, comma-separated origin list.
    @Value("${doctrip.cors.allowed-origin:http://localhost:4200}")
    private String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/public/**")
                .allowedOrigins(java.util.Arrays.stream(allowedOrigin.split(",")).map(String::trim).toArray(String[]::new))
                .allowedMethods("GET");
    }
}
