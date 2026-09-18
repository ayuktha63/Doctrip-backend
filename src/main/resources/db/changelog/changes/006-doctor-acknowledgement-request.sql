--liquibase formatted sql

--changeset doctrip:006-doctor-acknowledgement-request
CREATE TABLE doctor_acknowledgement_request (
    id UUID PRIMARY KEY,
    appointment_request_id UUID NOT NULL,
    doctor_id UUID NOT NULL,
    doctor_name VARCHAR(200) NOT NULL,
    doctor_specialty VARCHAR(150),
    status VARCHAR(30) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    response_deadline TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_doctor_ack_request_appointment FOREIGN KEY (appointment_request_id) REFERENCES appointment_request (id)
);

--changeset doctrip:006-doctor-acknowledgement-request-index
CREATE INDEX ix_doctor_ack_request_appointment ON doctor_acknowledgement_request (appointment_request_id);
