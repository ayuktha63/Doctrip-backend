package com.doctrip.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One outstanding (or resolved) acknowledgement request sent to one doctor for one appointment
 * request (FR-02/FR-04/FR-05). {@code doctorId}/{@code doctorName}/{@code doctorSpecialty} are a
 * snapshot at send time, not a live FK — real Doctor master data doesn't exist yet in this backend
 * (see CLAUDE.md non-goals); the id/name just need to be stable enough to identify which doctor
 * this row is for once real Doctor data lands.
 */
@Entity
@Table(name = "doctor_acknowledgement_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorAcknowledgementRequest {

    @Id
    private UUID id;

    @Column(name = "appointment_request_id", nullable = false)
    private UUID appointmentRequestId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "doctor_specialty")
    private String doctorSpecialty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DoctorAcknowledgementStatus status = DoctorAcknowledgementStatus.PENDING;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "response_deadline", nullable = false)
    private Instant responseDeadline;

    /** Set only when {@code status == ALTERNATIVE_PROPOSED} (or once accepted from that state) — FR-08. */
    @Column(name = "proposed_date")
    private LocalDate proposedDate;

    @Column(name = "proposed_time")
    private LocalTime proposedTime;

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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
