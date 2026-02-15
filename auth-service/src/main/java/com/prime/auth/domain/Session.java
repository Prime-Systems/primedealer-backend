package com.prime.auth.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Session entity stored in Redis.
 * Represents an active authentication session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session implements Serializable {

    private String sessionId;
    private UUID userId;
    private String email;
    private Set<String> roles;
    
    private Instant createdAt;
    private Instant lastActivityAt;
    private Instant expiresAt;
    
    private String ipAddress;
    private String userAgent;
    private String deviceFingerprint;
    
    private boolean mfaVerified;
    private String mfaMethod;
    
    private String refreshTokenHash;
    private int refreshCount;
    
    private boolean revoked;
    private Instant revokedAt;
    private String revokedReason;

    /**
     * Check if the session is valid.
     */
    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }

    /**
     * Check if the session needs MFA verification.
     */
    public boolean needsMfaVerification() {
        return !mfaVerified;
    }

    /**
     * Update last activity timestamp.
     */
    public void updateActivity() {
        this.lastActivityAt = Instant.now();
    }

    /**
     * Record a token refresh.
     */
    public void recordRefresh(String newRefreshTokenHash) {
        this.refreshTokenHash = newRefreshTokenHash;
        this.refreshCount++;
        this.lastActivityAt = Instant.now();
    }

    /**
     * Revoke the session.
     */
    public void revoke(String reason) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revokedReason = reason;
    }

    /**
     * Mark MFA as verified.
     */
    public void markMfaVerified() {
        this.mfaVerified = true;
    }
}
