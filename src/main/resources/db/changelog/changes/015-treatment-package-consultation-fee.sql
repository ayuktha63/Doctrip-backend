--liquibase formatted sql

--changeset doctrip:015-treatment-package-consultation-fee
ALTER TABLE treatment_packages ADD COLUMN consultation_fee NUMERIC;
--rollback ALTER TABLE treatment_packages DROP COLUMN consultation_fee;
