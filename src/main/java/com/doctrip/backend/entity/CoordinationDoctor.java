package com.doctrip.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal placeholder for Doctor master data usable for Hospital Admin appointment coordination
 * (FR-02/FR-04/FR-05) — eligibility (hospital/department/availability), not a doctor profile.
 *
 * <p>Named {@code CoordinationDoctor} (not just {@code Doctor}) specifically to avoid colliding
 * with Story 2's {@code com.doctrip.backend.doctor.Doctor}/{@code DoctorRepository} (public
 * discovery — profiles, specialties, browsing), which live in a different package with a
 * completely different shape and purpose (their table is {@code doctors}, plural; this one is
 * {@code coordination_doctor}). Spring names beans from the simple class name regardless of
 * package, so two classes both simply called {@code Doctor}/{@code DoctorRepository} would collide
 * at context-startup with a {@code BeanDefinitionOverrideException} — this class exists precisely
 * so that doesn't happen. {@link com.doctrip.backend.service.AppointmentRequestService} depends on
 * {@link com.doctrip.backend.repository.CoordinationDoctorRepository} directly; when a real
 * Doctor-management story unifies these two concepts, reconcile them then rather than deleting
 * this one out from under the coordination flow with no replacement.
 */
@Entity
@Table(name = "coordination_doctor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordinationDoctor {

    @Id
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

    private String specialty;

    @Builder.Default
    @Column(nullable = false)
    private boolean available = true;
}
