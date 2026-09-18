--liquibase formatted sql

--changeset doctrip:014-hospital-audit-log
CREATE TABLE hospital_audit_log (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    actor_type VARCHAR(50) NOT NULL,
    actor_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    action VARCHAR(50) NOT NULL,
    previous_value TEXT,
    new_value TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT now()
);
--rollback DROP TABLE hospital_audit_log;

--changeset doctrip:014-hospital-audit-log-index
CREATE INDEX ix_hospital_audit_log_hospital_entity ON hospital_audit_log (hospital_id, entity_type, entity_id);
--rollback DROP INDEX ix_hospital_audit_log_hospital_entity;
