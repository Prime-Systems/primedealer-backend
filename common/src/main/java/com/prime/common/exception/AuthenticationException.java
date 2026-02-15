package com.prime.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for authentication failures.
 */
public class AuthenticationException extends BaseException {

    public static final String INVALID_CREDENTIALS = "AUTH_001";
    public static final String ACCOUNT_LOCKED = "AUTH_002";
    public static final String ACCOUNT_DISABLED = "AUTH_003";
    public static final String TOKEN_EXPIRED = "AUTH_004";
    public static final String TOKEN_INVALID = "AUTH_005";
    public static final String MFA_REQUIRED = "AUTH_006";
    public static final String MFA_INVALID = "AUTH_007";
    public static final String SESSION_EXPIRED = "AUTH_008";
    public static final String INSUFFICIENT_PERMISSIONS = "AUTH_009";

    public AuthenticationException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED);
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid credentials", INVALID_CREDENTIALS);
    }

    public static AuthenticationException accountLocked() {
        return new AuthenticationException("Account is locked", ACCOUNT_LOCKED);
    }

    public static AuthenticationException accountDisabled() {
        return new AuthenticationException("Account is disabled", ACCOUNT_DISABLED);
    }

    public static AuthenticationException tokenExpired() {
        return new AuthenticationException("Token has expired", TOKEN_EXPIRED);
    }

    public static AuthenticationException tokenInvalid() {
        return new AuthenticationException("Token is invalid", TOKEN_INVALID);
    }

    public static AuthenticationException mfaRequired() {
        return new AuthenticationException("Multi-factor authentication required", MFA_REQUIRED);
    }

    public static AuthenticationException mfaInvalid() {
        return new AuthenticationException("Invalid MFA code", MFA_INVALID);
    }

    public static AuthenticationException sessionExpired() {
        return new AuthenticationException("Session has expired", SESSION_EXPIRED);
    }

    public static AuthenticationException insufficientPermissions() {
        return new AuthenticationException("Insufficient permissions", INSUFFICIENT_PERMISSIONS);
    }
}
