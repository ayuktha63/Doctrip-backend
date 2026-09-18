--liquibase formatted sql

--changeset doctrip:011-hospital-doctor
CREATE TABLE hospital_doctor (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    department_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    specialty VARCHAR(150),
    experience_years INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
--rollback DROP TABLE hospital_doctor;

--changeset doctrip:011-hospital-doctor-department-index
CREATE INDEX ix_hospital_doctor_department ON hospital_doctor (department_id);
--rollback DROP INDEX ix_hospital_doctor_department;

--changeset doctrip:011-hospital-doctor-hospital-name-index
CREATE UNIQUE INDEX ux_hospital_doctor_hospital_name ON hospital_doctor (hospital_id, lower(name));
--rollback DROP INDEX ux_hospital_doctor_hospital_name;
