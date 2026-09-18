package com.doctrip.backend.repository;

import com.doctrip.backend.entity.HospitalOperationalSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalOperationalSettingsRepository extends JpaRepository<HospitalOperationalSettings, UUID> {

    Optional<HospitalOperationalSettings> findByHospitalId(UUID hospitalId);
}
