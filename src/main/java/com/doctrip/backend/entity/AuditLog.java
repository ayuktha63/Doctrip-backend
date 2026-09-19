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
import java.util.UUID;

/**
 * Security/audit trail entries (registration, OTP, login, logout events) plus appointment
 * coordination events. Never carries an OTP value. {@code appointmentRequestId} is null for
 * Story 1's auth events and set for every event {@link com.doctrip.backend.service.AppointmentRequestService}
 * records, so a request's "Coordination log" can be filtered precisely instead of by patientId
 * alone (which would mix together every request a patient has ever made).
 */
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

    private UUID appointmentRequestId;

    @jakarta.persistence.Column(nullable = false)
    private String event;

    private String channel;

    @jakarta.persistence.Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();
}
