package com.prime.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * WebAuthn Passkey credential entity.
 * Stores passkey data for passwordless authentication.
 */
@Entity
@Table(name = "passkeys", indexes = {
        @Index(name = "idx_passkeys_user_id", columnList = "user_id"),
        @Index(name = "idx_passkeys_credential_id", columnList = "credential_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passkey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "credential_id", nullable = false, unique = true, length = 512)
    private String credentialId;

    @Column(name = "public_key_cose", nullable = false, columnDefinition = "BYTEA")
    private byte[] publicKeyCose;

    @Column(name = "sign_count", nullable = false)
    @Builder.Default
    private long signCount = 0;

    @Column(name = "aaguid", length = 36)
    private String aaguid;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "transports", length = 255)
    private String transports;

    @Column(name = "attestation_format", length = 50)
    private String attestationFormat;

    @Column(name = "user_verified", nullable = false)
    @Builder.Default
    private boolean userVerified = false;

    @Column(name = "backup_eligible", nullable = false)
    @Builder.Default
    private boolean backupEligible = false;

    @Column(name = "backup_state", nullable = false)
    @Builder.Default
    private boolean backupState = false;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Update sign count after successful authentication.
     */
    public void updateSignCount(long newSignCount) {
        if (newSignCount <= this.signCount) {
            throw new IllegalStateException("Possible credential cloning detected");
        }
        this.signCount = newSignCount;
        this.lastUsedAt = Instant.now();
    }

    /**
     * Revoke this passkey.
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }
}
