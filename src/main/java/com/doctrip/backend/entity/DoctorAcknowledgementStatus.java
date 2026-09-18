package com.doctrip.backend.entity;

/** One acknowledgement request's lifecycle (FR-06 responses are handled by a later slice — this
 * story only sends the request and observes PENDING/CLOSED for now). */
public enum DoctorAcknowledgementStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    ALTERNATIVE_PROPOSED,
    CLOSED
}
