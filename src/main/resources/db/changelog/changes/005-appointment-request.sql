--liquibase formatted sql

--changeset doctrip:005-appointment-request-code-seq
CREATE SEQUENCE appointment_request_code_seq START WITH 1 INCREMENT BY 1;

--changeset doctrip:005-appointment-request
CREATE TABLE appointment_request (
    id UUID PRIMARY KEY,
    request_code VARCHAR(20) NOT NULL UNIQUE,
    patient_id VARCHAR(64) NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    patient_country VARCHAR(120),
    patient_age INTEGER,
    patient_gender VARCHAR(30),
    hospital_id UUID NOT NULL,
    department VARCHAR(150) NOT NULL,
    requested_doctor_name VARCHAR(200),
    requested_date DATE NOT NULL,
    requested_time TIME NOT NULL,
    healthcare_requirement VARCHAR(2000) NOT NULL,
    additional_info VARCHAR(2000),
    status VARCHAR(40) NOT NULL,
    admin_sla_deadline TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_appointment_request_hospital FOREIGN KEY (hospital_id) REFERENCES hospital (id),
    CONSTRAINT fk_appointment_request_patient FOREIGN KEY (patient_id) REFERENCES patient (id)
);

--changeset doctrip:005-appointment-request-document
CREATE TABLE appointment_request_document (
    appointment_request_id UUID NOT NULL,
    document_name VARCHAR(200) NOT NULL,
    CONSTRAINT fk_appointment_request_document_request FOREIGN KEY (appointment_request_id) REFERENCES appointment_request (id) ON DELETE CASCADE
);

--changeset doctrip:005-appointment-request-hospital-index
CREATE INDEX ix_appointment_request_hospital ON appointment_request (hospital_id);
