package com.prime.common.security;

/**
 * Security-related constants shared across services.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    // HTTP Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String SESSION_ID_HEADER = "X-Session-ID";
    public static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String REAL_IP_HEADER = "X-Real-IP";
    
    // Aliases for compatibility
    public static final String HEADER_USER_ID = USER_ID_HEADER;
    public static final String HEADER_SESSION_ID = SESSION_ID_HEADER;
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    // JWT Claim Keys
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_SESSION_ID = "sessionId";
    public static final String CLAIM_MFA_VERIFIED = "mfaVerified";
    public static final String CLAIM_TOKEN_TYPE = "type";

    // Token Types
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String TOKEN_TYPE_PASSWORD_RESET = "password_reset";
    public static final String TOKEN_TYPE_EMAIL_VERIFICATION = "email_verification";
    public static final String TOKEN_TYPE_MFA_SETUP = "mfa_setup";

    // Rate Limiting
    public static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";
    public static final String RETRY_AFTER_HEADER = "Retry-After";

    // Endpoints
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/passkey/assertion/options",
            "/api/v1/auth/passkey/assertion/verify",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };
}
