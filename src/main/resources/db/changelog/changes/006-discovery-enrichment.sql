--liquibase formatted sql

--changeset doctrip:006-hospitals-enrichment
ALTER TABLE hospitals ADD COLUMN type VARCHAR(255);
ALTER TABLE hospitals ADD COLUMN city VARCHAR(255);
ALTER TABLE hospitals ADD COLUMN beds INTEGER;
ALTER TABLE hospitals ADD COLUMN rating NUMERIC(38,2);
ALTER TABLE hospitals ADD COLUMN review_count INTEGER;
ALTER TABLE hospitals ADD COLUMN accreditation VARCHAR(255);
ALTER TABLE hospitals ADD COLUMN languages VARCHAR(255);

--changeset doctrip:006-doctors-enrichment
ALTER TABLE doctors ADD COLUMN hospital_id BIGINT REFERENCES hospitals(id);
ALTER TABLE doctors ADD COLUMN department VARCHAR(255);
ALTER TABLE doctors ADD COLUMN specialty VARCHAR(255);
ALTER TABLE doctors ADD COLUMN experience_years INTEGER;
ALTER TABLE doctors ADD COLUMN rating NUMERIC(38,2);
ALTER TABLE doctors ADD COLUMN review_count INTEGER;
ALTER TABLE doctors ADD COLUMN languages VARCHAR(255);

--changeset doctrip:006-treatment-packages-enrichment
ALTER TABLE treatment_packages ADD COLUMN hospital_id BIGINT REFERENCES hospitals(id);
ALTER TABLE treatment_packages ADD COLUMN department VARCHAR(255);
ALTER TABLE treatment_packages ADD COLUMN price NUMERIC(38,2);
ALTER TABLE treatment_packages ADD COLUMN typical_stay VARCHAR(255);
