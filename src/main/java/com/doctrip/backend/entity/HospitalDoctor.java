package com.doctrip.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Story 4's hospital-admin-managed doctor concept — deliberately separate from Story 2's
 * {@code doctor.Doctor} (public discovery model). See CLAUDE.md / the Story 4 plan for why the
 * two "doctor" concepts coexist.
 */
@Entity
@Table(name = "hospital_doctor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalDoctor {

    @Id
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(nullable = false)
    private String name;

    private String specialty;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

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
