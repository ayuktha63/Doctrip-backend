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
@Table(name = "hospital")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hospital {

    @Id
    private UUID id;

    @Column(name = "hospital_code", nullable = false, unique = true)
    private String hospitalCode;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private String type = "Multi-speciality";

    @Column(nullable = false)
    private String city;

    @Builder.Default
    @Column(nullable = false)
    private String state = "Kerala";

    @Column(name = "registration_number")
    private String registrationNumber;

    private String accreditation;

    @Column(nullable = false)
    private int beds;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = false;

    /**
     * DocTrip Admin's "Hide data" toggle — distinct from {@code active}. Deactivation removes the
     * hospital from patient-facing discovery entirely; this flag never deletes or hides a row from
     * the database, it only greys out this hospital's own treatments/doctors/departments on
     * admin-facing screens (DocTrip Admin's hospital detail, and that hospital's own Hospital Admin
     * console) while still excluding them from public discovery, same as an inactive hospital.
     */
    @Builder.Default
    @Column(name = "data_hidden", nullable = false)
    private boolean dataHidden = false;

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
