--liquibase formatted sql

--changeset doctrip:020-hospital-data-hidden
-- DocTrip Admin's "Hide data" toggle — independent of `active`/Deactivate. Never removes a row;
-- it only greys out this hospital's own admin-facing screens while excluding it from public
-- discovery. Mirrored onto the discovery-side `hospitals` table so the same public-listing
-- filters that already check `approved` can also check this.
ALTER TABLE hospital ADD COLUMN data_hidden BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE hospitals ADD COLUMN data_hidden BOOLEAN NOT NULL DEFAULT FALSE;
