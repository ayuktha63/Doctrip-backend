--liquibase formatted sql

--changeset doctrip:002-external-identity
CREATE TABLE external_identity (
    id          VARCHAR(36) PRIMARY KEY,
    provider    VARCHAR(32) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    patient_id  VARCHAR(36) NOT NULL REFERENCES patient (id),
    linked_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_provider_external_id UNIQUE (provider, external_id)
);
