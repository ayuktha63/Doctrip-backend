--liquibase formatted sql

--changeset doctrip:001-init
CREATE TABLE patient (
    id               VARCHAR(36) PRIMARY KEY,
    full_name        VARCHAR(255) NOT NULL,
    email            VARCHAR(255) UNIQUE,
    mobile           VARCHAR(32) UNIQUE,
    password_hash    VARCHAR(255),
    email_verified   BOOLEAN NOT NULL DEFAULT FALSE,
    mobile_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    status           VARCHAR(32) NOT NULL DEFAULT 'pending_verification',
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_patient_email ON patient (email);
CREATE INDEX idx_patient_mobile ON patient (mobile);

CREATE TABLE patient_auth_providers (
    patient_id VARCHAR(36) NOT NULL REFERENCES patient (id),
    provider   VARCHAR(32) NOT NULL
);

CREATE TABLE otp_challenge (
    id           VARCHAR(36) PRIMARY KEY,
    patient_id   VARCHAR(36) NOT NULL,
    channel      VARCHAR(16) NOT NULL,
    purpose      VARCHAR(16) NOT NULL,
    target       VARCHAR(255) NOT NULL,
    code_hash    VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMP NOT NULL,
    attempts     INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    consumed     BOOLEAN NOT NULL DEFAULT FALSE,
    invalidated  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_patient_purpose ON otp_challenge (patient_id, purpose);

CREATE TABLE audit_log (
    id         VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36),
    event      VARCHAR(64) NOT NULL,
    channel    VARCHAR(16),
    timestamp  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
