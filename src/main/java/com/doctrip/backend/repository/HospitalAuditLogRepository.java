package com.doctrip.backend.repository;

import com.doctrip.backend.entity.HospitalAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalAuditLogRepository extends JpaRepository<HospitalAuditLog, UUID> {

    List<HospitalAuditLog> findByHospitalId(UUID hospitalId);
}
