package com.doctrip.backend.service;

import com.doctrip.backend.dto.hospitaladmin.OperationalSettingsRequest;
import com.doctrip.backend.dto.hospitaladmin.OperationalSettingsResponse;
import com.doctrip.backend.entity.HospitalOperationalSettings;
import com.doctrip.backend.repository.HospitalOperationalSettingsRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalOperationalSettingsService {

    private final HospitalOperationalSettingsRepository hospitalOperationalSettingsRepository;
    private final HospitalAuditService hospitalAuditService;

    public HospitalOperationalSettingsService(
            HospitalOperationalSettingsRepository hospitalOperationalSettingsRepository,
            HospitalAuditService hospitalAuditService) {
        this.hospitalOperationalSettingsRepository = hospitalOperationalSettingsRepository;
        this.hospitalAuditService = hospitalAuditService;
    }

    public OperationalSettingsResponse get(UUID hospitalId) {
        return OperationalSettingsResponse.from(
                hospitalId, hospitalOperationalSettingsRepository.findByHospitalId(hospitalId).orElse(null));
    }

    @Transactional
    public OperationalSettingsResponse upsert(UUID hospitalId, UUID actorId, OperationalSettingsRequest request) {
        HospitalOperationalSettings settings = hospitalOperationalSettingsRepository
                .findByHospitalId(hospitalId)
                .orElseGet(() -> HospitalOperationalSettings.builder().hospitalId(hospitalId).build());

        OperationalSettingsResponse before = OperationalSettingsResponse.from(
                hospitalId, settings.getId() == null ? null : settings);

        settings.setSla(request.sla());
        settings.setCoordinationWindow(request.coordinationWindow());
        settings.setEscalationContact(request.escalationContact());
        settings.setLanguagesSupported(request.languagesSupported());
        settings.setMinLeadTime(request.minLeadTime());
        settings.setInterpreterAvailability(request.interpreterAvailability());
        settings = hospitalOperationalSettingsRepository.save(settings);

        OperationalSettingsResponse after = OperationalSettingsResponse.from(hospitalId, settings);
        hospitalAuditService.record(
                hospitalId, actorId, "OPERATIONAL_SETTINGS", settings.getId(), "UPDATE", before, after);
        return after;
    }
}
