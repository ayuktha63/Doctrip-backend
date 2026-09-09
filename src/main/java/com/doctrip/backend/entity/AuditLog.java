package com.doctrip.backend.entity;

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

/** Security/audit trail entries (registration, OTP, login, logout events). Never carries an OTP value. */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String patientId;

    @jakarta.persistence.Column(nullable = false)
    private String event;

    private String channel;

    @jakarta.persistence.Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();
}
