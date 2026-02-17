-- Flyway migration to add id_verification_status to users table
ALTER TABLE users ADD COLUMN id_verification_status BOOLEAN NOT NULL DEFAULT FALSE;
