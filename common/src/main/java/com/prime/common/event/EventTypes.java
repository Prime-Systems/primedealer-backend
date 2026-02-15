package com.prime.common.event;

/**
 * Event types following domain-driven design principles.
 * Events are facts about what happened, not commands.
 */
public final class EventTypes {

    private EventTypes() {
        // Prevent instantiation
    }

    // User Lifecycle Events
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_VERIFIED = "user.verified";
    public static final String USER_UPDATED = "user.updated";
    public static final String USER_DELETED = "user.deleted";
    public static final String USER_LOCKED = "user.locked";
    public static final String USER_UNLOCKED = "user.unlocked";

    // Credential Events
    public static final String PASSWORD_CHANGED = "user.password.changed";
    public static final String PASSWORD_RESET_REQUESTED = "user.password.reset_requested";
    public static final String PASSWORD_RESET_COMPLETED = "user.password.reset_completed";

    // MFA Events
    public static final String MFA_ENABLED = "user.mfa.enabled";
    public static final String MFA_DISABLED = "user.mfa.disabled";
    public static final String MFA_VERIFIED = "user.mfa.verified";
    public static final String MFA_BACKUP_CODES_GENERATED = "user.mfa.backup_codes_generated";
    public static final String MFA_BACKUP_CODE_USED = "user.mfa.backup_code_used";

    // Passkey Events
    public static final String PASSKEY_REGISTERED = "user.passkey.registered";
    public static final String PASSKEY_REMOVED = "user.passkey.removed";
    public static final String PASSKEY_USED = "user.passkey.used";

    // Authentication Events
    public static final String LOGIN_SUCCEEDED = "auth.login.succeeded";
    public static final String LOGIN_FAILED = "auth.login.failed";
    public static final String LOGOUT = "auth.logout";
    public static final String TOKEN_ISSUED = "auth.token.issued";
    public static final String TOKEN_REFRESHED = "auth.token.refreshed";
    public static final String TOKEN_REVOKED = "auth.token.revoked";

    // Session Events
    public static final String SESSION_CREATED = "auth.session.created";
    public static final String SESSION_EXTENDED = "auth.session.extended";
    public static final String SESSION_TERMINATED = "auth.session.terminated";
    public static final String SESSION_EXPIRED = "auth.session.expired";

    // Security Events
    public static final String SUSPICIOUS_ACTIVITY_DETECTED = "security.suspicious_activity";
    public static final String BRUTE_FORCE_DETECTED = "security.brute_force";
    public static final String ACCOUNT_COMPROMISED = "security.account_compromised";
}
