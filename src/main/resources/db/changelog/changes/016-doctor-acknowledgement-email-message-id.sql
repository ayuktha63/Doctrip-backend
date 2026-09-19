--liquibase formatted sql

--changeset doctrip:016-doctor-acknowledgement-email-message-id
ALTER TABLE doctor_acknowledgement_request ADD COLUMN email_message_id VARCHAR(320);
--rollback ALTER TABLE doctor_acknowledgement_request DROP COLUMN email_message_id;

--changeset doctrip:016-doctor-acknowledgement-email-message-id-index
CREATE INDEX ix_doctor_acknowledgement_request_email_message_id ON doctor_acknowledgement_request (email_message_id);
--rollback DROP INDEX ix_doctor_acknowledgement_request_email_message_id;

--changeset doctrip:016-coordination-doctor-email
ALTER TABLE coordination_doctor ADD COLUMN email VARCHAR(320);
--rollback ALTER TABLE coordination_doctor DROP COLUMN email;
