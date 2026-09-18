--liquibase formatted sql

--changeset doctrip:007-discovery-hospital-admin-link
ALTER TABLE hospitals ADD COLUMN admin_hospital_id UUID;
ALTER TABLE hospitals ADD CONSTRAINT ux_hospitals_admin_hospital_id UNIQUE (admin_hospital_id);
