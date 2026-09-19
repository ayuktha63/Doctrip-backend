--liquibase formatted sql

--changeset doctrip:016-audit-log-appointment-request-id
-- The "Coordination log" (mockup's isCoord cur.log) must show only the events for ONE
-- appointment request. audit_log was previously matched to a request only via patient_id, which
-- breaks the moment a patient has more than one request (their history mixes together) — this
-- column lets AppointmentRequestService.getLog filter precisely. Nullable: Story 1's auth events
-- (registration/OTP/login) aren't tied to any appointment request and never will be.
ALTER TABLE audit_log ADD COLUMN appointment_request_id UUID;

--changeset doctrip:016-audit-log-appointment-request-id-index
CREATE INDEX idx_audit_log_appointment_request_id ON audit_log (appointment_request_id);
