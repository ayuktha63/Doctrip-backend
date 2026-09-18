package com.doctrip.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hospital_operational_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalOperationalSettings {

    @Id
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    private String sla;

    @Column(name = "coordination_window")
    private String coordinationWindow;

    @Column(name = "escalation_contact")
    private String escalationContact;

    @Column(name = "languages_supported")
    private String languagesSupported;

    @Column(name = "min_lead_time")
    private String minLeadTime;

    @Column(name = "interpreter_availability")
    private String interpreterAvailability;

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
