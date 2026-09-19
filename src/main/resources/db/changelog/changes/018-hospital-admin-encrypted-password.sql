--liquibase formatted sql

--changeset doctrip:018-hospital-admin-encrypted-password
-- Reversible copy of the password alongside the existing one-way password_hash (still used for
-- actual login verification) so DocTrip Admin can view a Hospital Admin's current password on
-- request — see CredentialEncryptionService for the security trade-off this represents.
ALTER TABLE hospital_admin ADD COLUMN encrypted_password TEXT;
