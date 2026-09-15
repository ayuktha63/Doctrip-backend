package com.doctrip.backend.service;

import com.doctrip.backend.entity.AuditLog;
import com.doctrip.backend.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists + logs security/audit events. Never pass an OTP value or password into event/channel.
 * Runs in its own transaction (REQUIRES_NEW) so an audit record (e.g. otp_verify_failed,
 * otp_locked) survives even when the caller's transaction subsequently rolls back after throwing.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String patientId, String event, String channel) {
        auditLogRepository.save(AuditLog.builder()
                .patientId(patientId)
                .event(event)
                .channel(channel)
                .build());
        log.info("audit event={} patientId={} channel={}", event, patientId, channel);
    }
}
