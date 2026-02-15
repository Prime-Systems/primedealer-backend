package com.prime.user.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User aggregate root entity.
 * Manages user identity and encapsulates all user-related business rules.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_username", columnList = "username", unique = true),
        @Index(name = "idx_users_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;

    // MFA Configuration
    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;

    @Column(name = "mfa_method", length = 20)
    @Enumerated(EnumType.STRING)
    private MfaMethod mfaMethod;

    // Security
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    // Roles
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    // Audit
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<OAuthProvider> oauthProviders = new HashSet<>();

    @Version
    @Column(name = "version")
    private Long version;

    // New profile fields
//    @NotBlank(message = "Address is required")
    @Size(max = 500)
    @Column(nullable = true, length = 500)
    private String address;

//    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Column(nullable = true, length = 100)
    private String city;

//    @NotBlank(message = "State is required")
    @Size(max = 100)
    @Column(nullable = true, length = 100)
    private String state;

//    @NotBlank(message = "Zip code is required")
    @Size(max = 20)
    @Column(name = "zip_code", nullable = true, length = 20)
    private String zipCode;

    @Size(max = 2000)
    @Column(columnDefinition = "TEXT")
    private String bio;

    @Size(max = 500)
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

//    @NotBlank(message = "Ghana card number is required")
    @Size(max = 50)
    @Column(name = "ghana_card_number", nullable = true, unique = true, length = 50)
    private String ghanaCardNumber;



    // Domain Methods

    /**
     * Check if the account is currently locked.
     */
    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    /**
     * Check if the account is active and can perform actions.
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE && !isLocked();
    }

    /**
     * Record a failed login attempt.
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = Instant.now().plusSeconds(900); // 15 minutes
        }
    }

    /**
     * Record a successful login.
     */
    public void recordSuccessfulLogin(String ip) {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = Instant.now();
        this.lastLoginIp = ip;
    }

    /**
     * Unlock the account.
     */
    public void unlock() {
        this.lockedUntil = null;
        this.failedLoginAttempts = 0;
    }

    /**
     * Verify the email address.
     */
    public void verifyEmail() {
        this.emailVerified = true;
        if (this.status == UserStatus.PENDING_VERIFICATION) {
            this.status = UserStatus.ACTIVE;
        }
    }

    /**
     * Enable MFA with TOTP.
     */
    public void enableMfa(String secret) {
        this.mfaEnabled = true;
        this.mfaSecret = secret;
        this.mfaMethod = MfaMethod.TOTP;
    }

    /**
     * Disable MFA.
     */
    public void disableMfa() {
        this.mfaEnabled = false;
        this.mfaSecret = null;
        this.mfaMethod = null;
    }

    /**
     * Change password.
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = Instant.now();
    }

    /**
     * Get full name.
     */
    public String getFullName() {
        if (firstName == null && lastName == null) return username;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }

    public enum UserStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        SUSPENDED,
        DEACTIVATED
    }

    public enum MfaMethod {
        TOTP,
        SMS,
        EMAIL
    }
}
