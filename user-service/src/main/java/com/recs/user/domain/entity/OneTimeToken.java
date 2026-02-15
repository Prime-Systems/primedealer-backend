package com.prime.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * One-time token entity for password reset, email verification, etc.
 * Implements secure token handling with expiration and single-use semantics.
 */
@Entity
@Table(name = "one_time_tokens", indexes = {
        @Index(name = "idx_one_time_tokens_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_one_time_tokens_user_id", columnList = "user_id"),
        @Index(name = "idx_one_time_tokens_type", columnList = "token_type"),
        @Index(name = "idx_one_time_tokens_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneTimeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 30)
    private TokenType tokenType;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Check if this token is valid (not expired and not used).
     */
    public boolean isValid() {
        return !used && Instant.now().isBefore(expiresAt);
    }

    /**
     * Mark this token as used.
     */
    public void markUsed() {
        this.used = true;
        this.usedAt = Instant.now();
    }

    public enum TokenType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        PHONE_VERIFICATION,
        MFA_SETUP,
        ACCOUNT_RECOVERY,
        LOGIN_VERIFICATION
    }
}
