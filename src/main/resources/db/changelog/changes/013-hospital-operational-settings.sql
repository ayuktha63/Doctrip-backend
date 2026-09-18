--liquibase formatted sql

--changeset doctrip:013-hospital-operational-settings
CREATE TABLE hospital_operational_settings (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    sla VARCHAR(500),
    coordination_window VARCHAR(500),
    escalation_contact VARCHAR(500),
    languages_supported VARCHAR(500),
    min_lead_time VARCHAR(500),
    interpreter_availability VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
--rollback DROP TABLE hospital_operational_settings;

--changeset doctrip:013-hospital-operational-settings-hospital-index
CREATE UNIQUE INDEX ux_hospital_operational_settings_hospital ON hospital_operational_settings (hospital_id);
--rollback DROP INDEX ux_hospital_operational_settings_hospital;
