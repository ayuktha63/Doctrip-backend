--liquibase formatted sql

-- Minimal Doctor placeholder for Hospital Admin appointment coordination — see
-- entity/CoordinationDoctor.java's doc comment for why this is a separate table from Story 2's
-- `doctors` (public discovery), not a rename of it.

--changeset doctrip:008-coordination-doctor-minimal
CREATE TABLE coordination_doctor (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    department VARCHAR(150) NOT NULL,
    specialty VARCHAR(150),
    available BOOLEAN NOT NULL DEFAULT TRUE
);

--changeset doctrip:008-coordination-doctor-hospital-department-index
CREATE INDEX ix_coordination_doctor_hospital_department ON coordination_doctor (hospital_id, department);
