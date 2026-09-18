package com.doctrip.backend.hospital;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hospitals")
@Getter
@Setter
@NoArgsConstructor
public class DiscoveryHospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean approved = false;

    private String type;

    private String city;

    private Integer beds;

    private BigDecimal rating;

    private Integer reviewCount;

    private String accreditation;

    private String languages;

    /**
     * Links this discovery-side row to the admin console's {@code entity.Hospital} (Story 3)
     * that created it, so AdminHospitalSyncService can find-or-create idempotently. Null for
     * any discovery hospital not sourced from the admin console (e.g. seeded directly).
     */
    @Column(name = "admin_hospital_id", unique = true)
    private UUID adminHospitalId;
}
