--liquibase formatted sql

--changeset doctrip:009-hospital-admin
CREATE TABLE hospital_admin (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    email VARCHAR(180) NOT NULL,
    password_hash VARCHAR(200) NOT NULL,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
--rollback DROP TABLE hospital_admin;

--changeset doctrip:009-hospital-admin-email-index
CREATE UNIQUE INDEX ux_hospital_admin_email ON hospital_admin (email);
--rollback DROP INDEX ux_hospital_admin_email;
