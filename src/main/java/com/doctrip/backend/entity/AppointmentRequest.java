package com.doctrip.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Patient's appointment request as routed to a Hospital, and everything Hospital Admin needs to
 * review it (FR-01). Patient contact fields ({@code patientName}/{@code patientCountry}/{@code
 * patientAge}/{@code patientGender}) are a snapshot taken at request time, not a live join to
 * {@link Patient} — deliberate, since a request also carries booking-specific patient context (e.g.
 * age/gender/country relevant to this particular booking) that doesn't belong on the permanent
 * patient profile. {@code department}/{@code requestedDoctorName} are plain free-text, not FKs —
 * this story doesn't build Department or Doctor master data (see the backend CLAUDE.md's
 * non-goals).
 */
@Entity
@Table(name = "appointment_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    @Id
    private UUID id;

    @Column(name = "request_code", nullable = false, unique = true)
    private String requestCode;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "patient_country")
    private String patientCountry;

    @Column(name = "patient_age")
    private Integer patientAge;

    @Column(name = "patient_gender")
    private String patientGender;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(nullable = false)
    private String department;

    @Column(name = "requested_doctor_name")
    private String requestedDoctorName;

    @Column(name = "requested_date", nullable = false)
    private LocalDate requestedDate;

    @Column(name = "requested_time", nullable = false)
    private LocalTime requestedTime;

    @Column(name = "healthcare_requirement", nullable = false, length = 2000)
    private String healthcareRequirement;

    @Column(name = "additional_info", length = 2000)
    private String additionalInfo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "appointment_request_document", joinColumns = @JoinColumn(name = "appointment_request_id"))
    @Column(name = "document_name")
    @Builder.Default
    private List<String> requiredDocuments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentRequestStatus status = AppointmentRequestStatus.REQUESTED;

    @Column(name = "admin_sla_deadline", nullable = false)
    private Instant adminSlaDeadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (adminSlaDeadline == null) adminSlaDeadline = now.plusSeconds(3600);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
