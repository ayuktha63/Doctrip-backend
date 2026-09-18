package com.doctrip.backend.repository;

import com.doctrip.backend.entity.DoctorAcknowledgementRequest;
import com.doctrip.backend.entity.DoctorAcknowledgementStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorAcknowledgementRequestRepository extends JpaRepository<DoctorAcknowledgementRequest, UUID> {

    List<DoctorAcknowledgementRequest> findByAppointmentRequestIdOrderBySentAtAsc(UUID appointmentRequestId);

    List<DoctorAcknowledgementRequest> findByAppointmentRequestIdAndStatus(
            UUID appointmentRequestId, DoctorAcknowledgementStatus status);

    /** BR-13 — close every other still-open ack once one has won (FR-07) or an alternative was
     * selected (FR-08). {@code excludeId} is the winner, left untouched. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DoctorAcknowledgementRequest a set a.status = :closed, a.updatedAt = :now "
            + "where a.appointmentRequestId = :requestId and a.id <> :excludeId "
            + "and a.status in :openStatuses")
    int closeOthers(
            @Param("requestId") UUID requestId,
            @Param("excludeId") UUID excludeId,
            @Param("openStatuses") List<DoctorAcknowledgementStatus> openStatuses,
            @Param("closed") DoctorAcknowledgementStatus closed,
            @Param("now") Instant now);

    long countByAppointmentRequestIdAndStatusIn(UUID appointmentRequestId, List<DoctorAcknowledgementStatus> statuses);

    /** EC-12 — candidates for response-window timeout: never responded, deadline already passed. */
    List<DoctorAcknowledgementRequest> findByStatusAndResponseDeadlineBefore(DoctorAcknowledgementStatus status, Instant now);

    /** FR-11 — patient cancellation closes every still-relevant ack for the request, including an
     * already-ACCEPTED one (BR-20 — it was never Confirmed, so cancellation can still terminate it). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DoctorAcknowledgementRequest a set a.status = :closed, a.updatedAt = :now "
            + "where a.appointmentRequestId = :requestId and a.status in :openStatuses")
    int closeAllOpen(
            @Param("requestId") UUID requestId,
            @Param("openStatuses") List<DoctorAcknowledgementStatus> openStatuses,
            @Param("closed") DoctorAcknowledgementStatus closed,
            @Param("now") Instant now);

    /** Guards a single ack against being responded to twice concurrently — mirrors
     * {@link AppointmentRequestRepository#compareAndSetStatus}. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DoctorAcknowledgementRequest a set a.status = :toStatus, a.updatedAt = :now "
            + "where a.id = :id and a.status = :fromStatus")
    int compareAndSetStatus(
            @Param("id") UUID id,
            @Param("fromStatus") DoctorAcknowledgementStatus fromStatus,
            @Param("toStatus") DoctorAcknowledgementStatus toStatus,
            @Param("now") Instant now);

    /** FR-08 — records the proposed slot and flips PENDING -> ALTERNATIVE_PROPOSED in one atomic
     * write, so it can't be clobbered by a stale in-memory entity the way a separate save() could. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DoctorAcknowledgementRequest a set a.status = :toStatus, a.proposedDate = :proposedDate, "
            + "a.proposedTime = :proposedTime, a.updatedAt = :now "
            + "where a.id = :id and a.status = :fromStatus")
    int proposeAlternative(
            @Param("id") UUID id,
            @Param("fromStatus") DoctorAcknowledgementStatus fromStatus,
            @Param("toStatus") DoctorAcknowledgementStatus toStatus,
            @Param("proposedDate") java.time.LocalDate proposedDate,
            @Param("proposedTime") java.time.LocalTime proposedTime,
            @Param("now") Instant now);
}
