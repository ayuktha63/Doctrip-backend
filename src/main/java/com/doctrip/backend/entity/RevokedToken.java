package com.doctrip.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * FR-09 — JWTs are stateless, so a logged-out access token stays valid until its natural expiry
 * unless denylisted. Every token carries a jti (JWT ID) claim; logout records it here, and
 * JwtAuthenticationFilter rejects any presented token whose jti is present in this table.
 */
@Entity
@Table(name = "revoked_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Instant revokedAt = Instant.now();
}
