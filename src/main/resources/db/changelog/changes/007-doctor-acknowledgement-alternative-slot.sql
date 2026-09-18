--liquibase formatted sql

--changeset doctrip:007-doctor-acknowledgement-alternative-slot
ALTER TABLE doctor_acknowledgement_request ADD COLUMN proposed_date DATE;
ALTER TABLE doctor_acknowledgement_request ADD COLUMN proposed_time TIME;
