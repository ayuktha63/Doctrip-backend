--liquibase formatted sql

--changeset doctrip:008-backfill-discovery-hospitals
-- Backfills the Story 2 discovery table (hospitals) for admin hospitals (hospital) created
-- before HospitalService.syncDiscoveryHospital existed, so activated hospitals that predate
-- the sync don't require a manual re-toggle to become discoverable. ON CONFLICT is a no-op
-- safeguard in case this ever re-runs against a row already synced by the application.
INSERT INTO hospitals (admin_hospital_id, name, type, city, beds, accreditation, approved)
SELECT h.id, h.name, h.type, h.city, h.beds, h.accreditation, h.active
FROM hospital h
WHERE NOT EXISTS (
    SELECT 1 FROM hospitals dh WHERE dh.admin_hospital_id = h.id
)
ON CONFLICT (admin_hospital_id) DO NOTHING;
