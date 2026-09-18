--liquibase formatted sql

--changeset doctrip:008-backfill-discovery-hospitals
-- Backfills the Story 2 discovery table (hospitals) for admin hospitals (hospital) created
-- before HospitalService.syncDiscoveryHospital existed, so activated hospitals that predate
-- the sync don't require a manual re-toggle to become discoverable. The WHERE NOT EXISTS guard
-- already prevents inserting a duplicate on any re-run (Liquibase changesets execute once,
-- sequentially, so there's no concurrent-insert race here to also need ON CONFLICT for) — that
-- clause was dropped because H2's PostgreSQL-compatibility mode (used under the "test" profile)
-- doesn't support ON CONFLICT after an INSERT ... SELECT, which broke every test's Liquibase
-- migration; real Postgres behavior is unaffected since WHERE NOT EXISTS alone is sufficient.
INSERT INTO hospitals (admin_hospital_id, name, type, city, beds, accreditation, approved)
SELECT h.id, h.name, h.type, h.city, h.beds, h.accreditation, h.active
FROM hospital h
WHERE NOT EXISTS (
    SELECT 1 FROM hospitals dh WHERE dh.admin_hospital_id = h.id
);
