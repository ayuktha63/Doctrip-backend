--liquibase formatted sql

--changeset doctrip:012-doctor-availability
CREATE TABLE doctor_availability (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    kind VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_doctor_availability_time_order CHECK (end_time > start_time)
);
--rollback DROP TABLE doctor_availability;

--changeset doctrip:012-doctor-availability-doctor-date-index
CREATE INDEX ix_doctor_availability_doctor_date ON doctor_availability (doctor_id, slot_date);
--rollback DROP INDEX ix_doctor_availability_doctor_date;
