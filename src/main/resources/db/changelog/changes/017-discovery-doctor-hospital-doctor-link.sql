--liquibase formatted sql

--changeset doctrip:017-discovery-doctor-hospital-doctor-link
-- Same disconnect as the earlier hospitals sync (007/008): Story 4's HospitalDoctorService wrote
-- only to its own hospital_doctor table, never to Story 2's public-discovery doctors table, so a
-- doctor added in the Hospital Admin console never showed up in patient-facing Discover.
ALTER TABLE doctors ADD COLUMN hospital_doctor_id UUID;
ALTER TABLE doctors ADD CONSTRAINT ux_doctors_hospital_doctor_id UNIQUE (hospital_doctor_id);

--changeset doctrip:017-backfill-discovery-doctors
-- Backfills any hospital_doctor rows created before this sync existed, same reasoning as
-- 008-backfill-discovery-hospitals.sql.
INSERT INTO doctors (hospital_doctor_id, name, department, specialty, experience_years, hospital_id, approved)
SELECT hd.id, hd.name, d.name, hd.specialty, hd.experience_years, dh.id, hd.active
FROM hospital_doctor hd
JOIN department d ON d.id = hd.department_id
JOIN hospitals dh ON dh.admin_hospital_id = hd.hospital_id
WHERE NOT EXISTS (
    SELECT 1 FROM doctors WHERE doctors.hospital_doctor_id = hd.id
);
