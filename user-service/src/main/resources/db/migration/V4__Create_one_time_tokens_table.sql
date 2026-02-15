-- V4__Create_one_time_tokens_table.sql
-- One-time tokens for password reset, email verification, etc.

CREATE TABLE one_time_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    token_type VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_token_type CHECK (token_type IN (
        'EMAIL_VERIFICATION', 
        'PASSWORD_RESET', 
        'PHONE_VERIFICATION', 
        'MFA_SETUP',
        'ACCOUNT_RECOVERY',
        'LOGIN_VERIFICATION'
    ))
);

-- Indexes
CREATE INDEX idx_one_time_tokens_token_hash ON one_time_tokens(token_hash);
CREATE INDEX idx_one_time_tokens_user_id ON one_time_tokens(user_id);
CREATE INDEX idx_one_time_tokens_type ON one_time_tokens(token_type);
CREATE INDEX idx_one_time_tokens_expires ON one_time_tokens(expires_at);
CREATE INDEX idx_one_time_tokens_active ON one_time_tokens(token_hash, token_type, used, expires_at) 
    WHERE used = FALSE;

-- Scheduled cleanup of expired tokens (run via cron job or pg_cron)
DELETE FROM one_time_tokens WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days' OR used = TRUE;
