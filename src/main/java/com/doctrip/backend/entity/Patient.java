package com.doctrip.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patient", indexes = {
        @Index(name = "idx_patient_email", columnList = "email"),
        @Index(name = "idx_patient_mobile", columnList = "mobile")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String fullName;

    /** Unique identity field. Nullable only for a patient who registered with mobile alone (not yet supported by FR-01's approved fields, kept nullable for OAuth-only identities). */
    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String mobile;

    /** Nullable when the patient has only ever authenticated via OAuth. */
    @Column
    private String passwordHash;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private boolean mobileVerified;

    /**
     * pending_verification: registered but required OTP verification not yet completed (FR-01/FR-03).
     * active: fully verified, authenticated access permitted.
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "pending_verification";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "patient_auth_providers", joinColumns = @JoinColumn(name = "patient_id"))
    @Column(name = "provider")
    @Builder.Default
    private List<String> authProviders = new ArrayList<>();

    /** When the patient accepted the Terms of Service/Privacy Policy at registration; null for OAuth-only accounts. */
    @Column
    private Instant termsAcceptedAt;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
