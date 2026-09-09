package com.doctrip.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    private final Key signingKey;
    private final long expirySeconds;

    public JwtService(@Value("${doctrip.jwt.secret}") String secret,
                       @Value("${doctrip.jwt.expiry-seconds:1800}") long expirySeconds) {
        // Ensure at least 256 bits for HS256.
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirySeconds = expirySeconds;
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }

    public String generateToken(String patientId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirySeconds * 1000);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(patientId)
                .issuedAt(now)
                .expiration(exp)
                .signWith((SecretKey) signingKey)
                .compact();
    }

    public String extractPatientId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
