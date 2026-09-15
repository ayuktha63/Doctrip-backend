package com.doctrip.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A single issued OTP challenge (FR-02/FR-05). The OTP value itself is never stored in
 * plaintext or logged (BR-06/audit trail requirement) — only its hash.
 */
@Entity
@Table(name = "otp_challenge", indexes = {
        @Index(name = "idx_otp_patient_purpose", columnList = "patientId, purpose")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String patientId;

    /** EMAIL or MOBILE — the channel this specific OTP was delivered through. */
    @Column(nullable = false)
    private String channel;

    /** REGISTER or LOGIN — which flow this OTP belongs to (an OTP is only valid for its own transaction). */
    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private String target;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(nullable = false)
    @Builder.Default
    private int maxAttempts = 5;

    @Column(nullable = false)
    @Builder.Default
    private boolean consumed = false;

    /** Set when a newer OTP is issued for the same patient+purpose (BR-07). */
    @Column(nullable = false)
    @Builder.Default
    private boolean invalidated = false;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !consumed && !invalidated && !isExpired() && attempts < maxAttempts;
    }
}
