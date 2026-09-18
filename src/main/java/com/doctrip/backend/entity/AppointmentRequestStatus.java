package com.doctrip.backend.entity;

/**
 * Appointment-request lifecycle status (see the Hospital Appointment Coordination story's business
 * rules). Only {@link #REQUESTED} is reachable so far — FR-01 delivers a request that Hospital Admin
 * can review, not the coordination transitions themselves. Later FRs in this story add the
 * remaining values (admin/system coordination, doctor acknowledgement, confirmation, escalation,
 * cancellation) as each is actually implemented — don't pre-add values a later FR hasn't built yet.
 */
public enum AppointmentRequestStatus {
    REQUESTED,
    DOCTOR_COORDINATION,
    /** A doctor/slot has been atomically secured (FR-07/FR-08) — still provisional (BR-20) until
     * Patient Confirmation, which is a separate not-yet-built slice of this story. */
    DOCTOR_SELECTED,
    /** No eligible doctor accepted and no valid alternative was proposed (FR-10). */
    ESCALATED,
    /** Patient cancelled before confirmation (FR-11) — terminal, releases the Booking Buffer. */
    CANCELLED,
    /** Patient Confirmation — the only status where the appointment is actually Confirmed (BR-20).
     * Terminal; no longer cancellable via FR-11 (that's only "before confirmation"). */
    CONFIRMED
}
