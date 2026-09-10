package com.doctrip.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Links a patient to an external OAuth identity (FR-06 Google, FR-07 Apple). */
@Entity
@Table(name = "external_identity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_provider_external_id", columnNames = {"provider", "externalId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    @Builder.Default
    private Instant linkedAt = Instant.now();
}
