--liquibase formatted sql

--changeset doctrip:003-refresh-and-revoked-tokens
CREATE TABLE refresh_token (
    id         VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_token_hash ON refresh_token (token_hash);
CREATE INDEX idx_refresh_token_patient ON refresh_token (patient_id);

CREATE TABLE revoked_token (
    id         VARCHAR(36) PRIMARY KEY,
    jti        VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
