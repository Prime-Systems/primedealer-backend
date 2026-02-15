-- Add user profile fields to the users table
ALTER TABLE users
    ADD COLUMN address VARCHAR(500)  NULL DEFAULT '',
    ADD COLUMN city VARCHAR(100)  NULL DEFAULT '',
    ADD COLUMN state VARCHAR(100)  NULL DEFAULT '',
    ADD COLUMN zip_code VARCHAR(20)  NULL DEFAULT '',
    ADD COLUMN bio TEXT NULL,
    ADD COLUMN profile_picture_url VARCHAR(500) NULL,
    ADD COLUMN ghana_card_number VARCHAR(50)  NULL DEFAULT '';

-- Remove default constraints after adding columns
ALTER TABLE users ALTER COLUMN address DROP DEFAULT;
ALTER TABLE users ALTER COLUMN city DROP DEFAULT;
ALTER TABLE users ALTER COLUMN state DROP DEFAULT;
ALTER TABLE users ALTER COLUMN zip_code DROP DEFAULT;
ALTER TABLE users ALTER COLUMN ghana_card_number DROP DEFAULT;

-- Add unique constraint for ghana_card_number
ALTER TABLE users
    ADD CONSTRAINT uk_users_ghana_card_number UNIQUE (ghana_card_number);

-- Create indexes for faster lookups
CREATE INDEX idx_users_ghana_card_number ON users(ghana_card_number);
CREATE INDEX idx_users_city ON users(city);
CREATE INDEX idx_users_state ON users(state);