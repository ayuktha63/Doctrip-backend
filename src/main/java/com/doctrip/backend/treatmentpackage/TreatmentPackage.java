package com.doctrip.backend.treatmentpackage;

import com.doctrip.backend.hospital.DiscoveryHospital;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "treatment_packages")
@Getter
@Setter
@NoArgsConstructor
public class TreatmentPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean approved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private DiscoveryHospital hospital;

    private String department;

    private BigDecimal price;

    private String typicalStay;

    /** Hospital-admin CRUD's "Consultation fee (₹)" field — not part of Story 2's original shape. */
    @Column(name = "consultation_fee")
    private BigDecimal consultationFee;
}
