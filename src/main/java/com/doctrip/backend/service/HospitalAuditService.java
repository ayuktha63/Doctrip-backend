package com.doctrip.backend.service;

import com.doctrip.backend.entity.HospitalAuditLog;
import com.doctrip.backend.repository.HospitalAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Brand-new audit trail independent from the patient-auth-only AuditLog/AuditService (wrong shape
 * for "who changed what field"). Called from inside the same @Transactional business method that
 * makes the change — an audit row must not exist if the change didn't commit.
 */
@Service
public class HospitalAuditService {

    private static final String ACTOR_TYPE_HOSPITAL_ADMIN = "HOSPITAL_ADMIN";

    private final HospitalAuditLogRepository hospitalAuditLogRepository;
    private final ObjectMapper objectMapper;

    public HospitalAuditService(HospitalAuditLogRepository hospitalAuditLogRepository, ObjectMapper objectMapper) {
        this.hospitalAuditLogRepository = hospitalAuditLogRepository;
        this.objectMapper = objectMapper;
    }

    /** Existing hospital-admin call sites — unchanged, still always actor type HOSPITAL_ADMIN. */
    public void record(
            UUID hospitalId, UUID actorId, String entityType, UUID entityId, String action,
            Object previousValue, Object newValue) {
        record(hospitalId, ACTOR_TYPE_HOSPITAL_ADMIN, actorId, entityType, entityId, action, previousValue, newValue);
    }

    /** For non-hospital-admin actors (e.g. the DocTrip platform admin, see BE-2.15). */
    public void record(
            UUID hospitalId, String actorType, UUID actorId, String entityType, UUID entityId, String action,
            Object previousValue, Object newValue) {
        // TODO: wire email once a provider exists
        HospitalAuditLog log = HospitalAuditLog.builder()
                .hospitalId(hospitalId)
                .actorType(actorType)
                .actorId(actorId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .previousValue(serialize(previousValue))
                .newValue(serialize(newValue))
                .build();
        hospitalAuditLogRepository.save(log);
    }

    private String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }
}
