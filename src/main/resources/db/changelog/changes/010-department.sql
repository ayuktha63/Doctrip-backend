--liquibase formatted sql

--changeset doctrip:010-department
CREATE TABLE department (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    head VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
--rollback DROP TABLE department;

--changeset doctrip:010-department-hospital-code-index
CREATE UNIQUE INDEX ux_department_hospital_code ON department (hospital_id, code);
--rollback DROP INDEX ux_department_hospital_code;
