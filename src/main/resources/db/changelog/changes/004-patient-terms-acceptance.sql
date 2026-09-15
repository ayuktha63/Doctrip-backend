--liquibase formatted sql

--changeset doctrip:004-patient-terms-acceptance
ALTER TABLE patient ADD COLUMN terms_accepted_at TIMESTAMP;
