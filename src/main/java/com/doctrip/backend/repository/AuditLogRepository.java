package com.doctrip.backend.repository;

import com.doctrip.backend.entity.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByAppointmentRequestIdOrderByTimestampAsc(UUID appointmentRequestId);
}
