package com.doctrip.backend.repository;

import com.doctrip.backend.entity.AppointmentRequest;
import com.doctrip.backend.entity.AppointmentRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRequestRepository extends JpaRepository<AppointmentRequest, UUID> {

    List<AppointmentRequest> findByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);

    /** FR-03 — candidates for System takeover: still awaiting Hospital Admin, past their SLA. */
    List<AppointmentRequest> findByStatusAndAdminSlaDeadlineBefore(AppointmentRequestStatus status, Instant now);

    @Query(value = "select nextval('appointment_request_code_seq')", nativeQuery = true)
    long nextRequestCodeSequence();

    /**
     * FR-07/BR-09/BR-11 — atomically flips the request from {@code fromStatus} to {@code toStatus}
     * only if it is still in {@code fromStatus}. Postgres row-locks the matching row for the
     * duration of this UPDATE, so under concurrent calls only one caller's UPDATE actually matches
     * and returns 1 — every other concurrent/late caller gets 0 and must treat itself as having
     * lost the race (AC-09/AC-10/AC-14), without needing an application-level lock.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppointmentRequest r set r.status = :toStatus, r.updatedAt = :now "
            + "where r.id = :id and r.status = :fromStatus")
    int compareAndSetStatus(
            @Param("id") UUID id,
            @Param("fromStatus") AppointmentRequestStatus fromStatus,
            @Param("toStatus") AppointmentRequestStatus toStatus,
            @Param("now") Instant now);

    /**
     * FR-11/AC-15/AC-16 — cancels the request unless it's already in a terminal status (CANCELLED
     * itself, or CONFIRMED — FR-11 is explicitly "before confirmation" only). Same atomic-guard
     * shape as {@link #compareAndSetStatus} — if a doctor's acceptance is racing this, whichever
     * UPDATE's row lock commits first wins, and the loser's own compare-and-set naturally fails (0
     * rows), which is exactly AC-16's "a later Doctor acceptance shall not create or confirm the
     * appointment."
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AppointmentRequest r set r.status = :cancelled, r.updatedAt = :now "
            + "where r.id = :id and r.status not in :terminalStatuses")
    int cancel(
            @Param("id") UUID id,
            @Param("cancelled") AppointmentRequestStatus cancelled,
            @Param("terminalStatuses") List<AppointmentRequestStatus> terminalStatuses,
            @Param("now") Instant now);
}
