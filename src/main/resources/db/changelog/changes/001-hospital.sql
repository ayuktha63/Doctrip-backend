--liquibase formatted sql

--changeset doctrip:001-hospital-code-seq
CREATE SEQUENCE hospital_code_seq START WITH 1 INCREMENT BY 1;

--changeset doctrip:001-hospital
CREATE TABLE hospital (
    id UUID PRIMARY KEY,
    hospital_code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(100) NOT NULL DEFAULT 'Multi-speciality',
    city VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL DEFAULT 'Kerala',
    registration_number VARCHAR(120),
    accreditation VARCHAR(200),
    beds INTEGER NOT NULL,
    contact_person VARCHAR(150),
    contact_email VARCHAR(180),
    contact_phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

--changeset doctrip:001-hospital-name-city-index
CREATE UNIQUE INDEX ux_hospital_name_city ON hospital (name, city);
