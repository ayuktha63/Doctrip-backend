--liquibase formatted sql

--changeset doctrip:019-hospital-doctor-email
-- Story 6/7's coordination flow emails a doctor at CoordinationDoctor.email, but Hospital Admin's
-- own "Add doctor" form never collected one — a real doctor added there could never actually be
-- eligible/reachable for coordination. Collected here so it can be synced through.
ALTER TABLE hospital_doctor ADD COLUMN email VARCHAR(320);

--changeset doctrip:019-coordination-doctor-hospital-doctor-link
-- Same disconnect as the earlier hospitals/doctors discovery sync (007/008, 017): HospitalDoctorService
-- wrote only to hospital_doctor, never to coordination_doctor, so a doctor added via Hospital Admin
-- was never actually eligible for appointment coordination — "Start coordination" stayed permanently
-- disabled (a.eligibleDoctors always empty) for any real doctor.
ALTER TABLE coordination_doctor ADD COLUMN hospital_doctor_id UUID;
ALTER TABLE coordination_doctor ADD CONSTRAINT ux_coordination_doctor_hospital_doctor_id UNIQUE (hospital_doctor_id);

--changeset doctrip:019-backfill-coordination-doctors
-- Backfills any hospital_doctor rows created before this sync existed.
INSERT INTO coordination_doctor (id, hospital_id, name, department, specialty, email, available, hospital_doctor_id)
SELECT gen_random_uuid(), hd.hospital_id, hd.name, d.name, hd.specialty, hd.email, hd.active, hd.id
FROM hospital_doctor hd
JOIN department d ON d.id = hd.department_id
WHERE NOT EXISTS (
    SELECT 1 FROM coordination_doctor WHERE coordination_doctor.hospital_doctor_id = hd.id
);
