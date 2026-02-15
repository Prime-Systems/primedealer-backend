package com.prime.auth.application.service;

import com.prime.auth.application.dto.*;
import com.prime.auth.domain.Session;
import com.prime.auth.domain.TokenPair;
import com.prime.auth.infrastructure.client.OAuthRegistrationRequest;
import com.prime.auth.infrastructure.client.UserInfo;
import com.prime.auth.infrastructure.client.UserServiceClient;
import com.prime.auth.infrastructure.repository.SessionRepository;
import com.prime.common.event.DomainEvent;
import com.prime.common.event.EventPublisher;
import com.prime.common.event.EventTypes;
import com.prime.common.exception.AuthenticationException;
import com.prime.common.security.JwtTokenProvider;
import com.prime.common.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Authentication service handling login, token management, and session orchestration.
 * Does NOT own user data - delegates to User Service for credential validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserServiceClient userServiceClient;
    private final SessionRepository sessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EventPublisher eventPublisher;

    @Value("${session.max-per-user:5}")
    private int maxSessionsPerUser;

    @Value("${session.duration-hours:168}")
    private int sessionDurationHours;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Authenticate user with OAuth.
     */
    public AuthResponse loginWithOAuth(OAuthRegistrationRequest request, 
                                        String ipAddress, String userAgent) {
        log.info("OAuth login attempt: email={}, provider={}", request.getEmail(), request.getProvider());

        Optional<UserInfo> userOpt = userServiceClient.processOAuth(request);

        if (userOpt.isEmpty()) {
            log.error("OAuth processing failed for email={}", request.getEmail());
            throw AuthenticationException.invalidCredentials();
        }

        UserInfo user = userOpt.get();

        // OAuth login bypasses MFA as providers usually handle it
        TokenPair tokenPair = createSession(user, ipAddress, userAgent, null, true);

        eventPublisher.publish(DomainEvent.authEvent(
                EventTypes.LOGIN_SUCCEEDED,
                user.getId().toString(),
                Map.of("ip", ipAddress, "method", "oauth", "provider", request.getProvider(), 
                        "sessionId", tokenPair.getSessionId())
        ));

        log.info("OAuth login successful: userId={}, provider={}", user.getId(), request.getProvider());
        return AuthResponse.fromTokenPair(tokenPair);
    }

    /**
     * Authenticate user with email and password.
     */
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt: email={}", request.getEmail());

        // Validate credentials via User Service
        Optional<UserInfo> userOpt = userServiceClient.validateCredentials(
                request.getEmail(), request.getPassword(), ipAddress);

        if (userOpt.isEmpty()) {
            log.warn("Login failed: invalid credentials for email={}", request.getEmail());
            
            eventPublisher.publish(DomainEvent.authEvent(
                    EventTypes.LOGIN_FAILED,
                    request.getEmail(),
                    Map.of("reason", "INVALID_CREDENTIALS", "ip", ipAddress)
            ));

            throw AuthenticationException.invalidCredentials();
        }

        UserInfo user = userOpt.get();

        // Check if MFA is required
        if (user.isMfaEnabled()) {
            if (request.getMfaCode() == null || request.getMfaCode().isEmpty()) {
                // Return MFA challenge
                return createMfaChallenge(user, ipAddress, userAgent, request.getDeviceFingerprint());
            }

            // Verify MFA code
            if (!userServiceClient.verifyMfa(user.getId(), request.getMfaCode())) {
                log.warn("Login failed: invalid MFA code for userId={}", user.getId());
                throw AuthenticationException.mfaInvalid();
            }
        }

        // Create session and tokens
        TokenPair tokenPair = createSession(user, ipAddress, userAgent, 
                request.getDeviceFingerprint(), true);

        eventPublisher.publish(DomainEvent.authEvent(
                EventTypes.LOGIN_SUCCEEDED,
                user.getId().toString(),
                Map.of("ip", ipAddress, "sessionId", tokenPair.getSessionId())
        ));

        log.info("Login successful: userId={}, sessionId={}", user.getId(), tokenPair.getSessionId());
        return AuthResponse.fromTokenPair(tokenPair);
    }

    /**
     * Authenticate user with passkey.
     */
    public AuthResponse loginWithPasskey(String email, Map<String, Object> credential,
                                          String ipAddress, String userAgent) {
        log.info("Passkey login attempt: email={}", email);

        Optional<UserInfo> userOpt = userServiceClient.verifyPasskey(email, credential);

        if (userOpt.isEmpty()) {
            log.warn("Passkey login failed for email={}", email);
            
            eventPublisher.publish(DomainEvent.authEvent(
                    EventTypes.LOGIN_FAILED,
                    email,
                    Map.of("reason", "INVALID_PASSKEY", "ip", ipAddress)
            ));

            throw AuthenticationException.invalidCredentials();
        }

        UserInfo user = userOpt.get();

        // Passkey login bypasses MFA (passkey itself is a form of strong authentication)
        TokenPair tokenPair = createSession(user, ipAddress, userAgent, null, true);

        eventPublisher.publish(DomainEvent.authEvent(
                EventTypes.LOGIN_SUCCEEDED,
                user.getId().toString(),
                Map.of("ip", ipAddress, "method", "passkey", "sessionId", tokenPair.getSessionId())
        ));

        log.info("Passkey login successful: userId={}", user.getId());
        return AuthResponse.fromTokenPair(tokenPair);
    }

    /**
     * Verify MFA and complete authentication.
     */
    public AuthResponse verifyMfa(MfaVerificationRequest request, String ipAddress, String userAgent) {
        log.info("MFA verification attempt");

        // Get pending session from MFA token
        Optional<Session> pendingSessionOpt = sessionRepository.consumeMfaToken(request.getMfaToken());

        if (pendingSessionOpt.isEmpty()) {
            log.warn("MFA verification failed: invalid or expired token");
            throw AuthenticationException.tokenExpired();
        }

        Session pendingSession = pendingSessionOpt.get();

        // Verify MFA code
        if (!userServiceClient.verifyMfa(pendingSession.getUserId(), request.getCode())) {
            log.warn("MFA verification failed: invalid code for userId={}", pendingSession.getUserId());
            throw AuthenticationException.mfaInvalid();
        }

        // Mark session as MFA verified and save
        pendingSession.markMfaVerified();
        pendingSession.setExpiresAt(Instant.now().plus(Duration.ofHours(sessionDurationHours)));
        sessionRepository.save(pendingSession);

        // Generate tokens
        TokenPair tokenPair = generateTokens(pendingSession);

        eventPublisher.publish(DomainEvent.authEvent(
                EventTypes.MFA_VERIFIED,
                pendingSession.getUserId().toString(),
                Map.of("sessionId", pendingSession.getSessionId())
        ));

        log.info("MFA verification successful: userId={}", pendingSession.getUserId());
        return AuthResponse.fromTokenPair(tokenPair);
    }

    /**
     * Refresh access token.
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Token refresh attempt");

        // Validate refresh token
        Optional<Claims> claimsOpt = jwtTokenProvider.validateRefreshToken(request.getRefreshToken());

        if (claimsOpt.isEmpty()) {
            log.warn("Token refresh failed: invalid token");
            throw AuthenticationException.tokenInvalid();
        }

        Claims claims = claimsOpt.get();
        String sessionId = claims.get(SecurityConstants.CLAIM_SESSION_ID, String.class);

        // Find session
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty() || !sessionOpt.get().isValid()) {
            log.warn("Token refresh failed: session not found or invalid for sessionId={}", sessionId);
            throw AuthenticationException.sessionExpired();
        }

        Session session = sessionOpt.get();

        // Verify refresh token hash matches
        String tokenHash = hashToken(request.getRefreshToken());
        if (!tokenHash.equals(session.getRefreshTokenHash())) {
            log.warn("Token refresh failed: token hash mismatch - possible token reuse");
            // Revoke session on token reuse (potential theft)
            session.revoke("Token reuse detected");
            sessionRepository.save(session);
            throw AuthenticationException.tokenInvalid();
        }

        // Get latest user info
        Optional<UserInfo> userOpt = userServiceClient.getUserAuthInfo(session.getUserId());
        if (userOpt.isEmpty()) {
            throw AuthenticationException.tokenInvalid();
        }

        UserInfo user = userOpt.get();

        // Generate new token pair
        TokenPair tokenPair = generateTokens(session, user);

        // Update session with new refresh token hash
        session.recordRefresh(hashToken(tokenPair.getRefreshToken()));
        sessionRepository.save(session);

        eventPublisher.publish(DomainEvent.authEvent(
                EventTypes.TOKEN_REFRESHED,
                session.getUserId().toString(),
                Map.of("sessionId", sessionId)
        ));

        log.debug("Token refresh successful: userId={}", session.getUserId());
        return AuthResponse.fromTokenPair(tokenPair);
    }

    /**
     * Logout and revoke session.
     */
    public void logout(String sessionId) {
        log.info("Logout: sessionId={}", sessionId);

        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            session.revoke("User logout");
            sessionRepository.save(session);
            sessionRepository.delete(sessionId);

            eventPublisher.publish(DomainEvent.authEvent(
                    EventTypes.LOGOUT,
                    session.getUserId().toString(),
                    Map.of("sessionId", sessionId)
            ));
        }
    }

    /**
     * Logout from all sessions.
     */
    public void logoutAll(UUID userId) {
        log.info("Logout all sessions: userId={}", userId);

        sessionRepository.deleteAllByUserId(userId);

        eventPublisher.publish(DomainEvent.authEvent(
                EventTypes.LOGOUT,
                userId.toString(),
                Map.of("scope", "ALL_SESSIONS")
        ));
    }

    /**
     * Get all active sessions for a user.
     */
    public List<SessionResponse> getSessions(UUID userId, String currentSessionId) {
        return sessionRepository.findByUserId(userId).stream()
                .map(s -> SessionResponse.fromSession(s, s.getSessionId().equals(currentSessionId)))
                .toList();
    }

    /**
     * Revoke a specific session.
     */
    public void revokeSession(UUID userId, String sessionId) {
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            if (session.getUserId().equals(userId)) {
                session.revoke("User revoked");
                sessionRepository.delete(sessionId);

                eventPublisher.publish(DomainEvent.authEvent(
                        EventTypes.SESSION_TERMINATED,
                        userId.toString(),
                        Map.of("sessionId", sessionId)
                ));
            }
        }
    }

    /**
     * Validate an access token and return session info.
     */
    public Optional<Session> validateToken(String accessToken) {
        Optional<Claims> claimsOpt = jwtTokenProvider.validateAccessToken(accessToken);

        if (claimsOpt.isEmpty()) {
            return Optional.empty();
        }

        Claims claims = claimsOpt.get();
        String sessionId = claims.get(SecurityConstants.CLAIM_SESSION_ID, String.class);

        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty() || !sessionOpt.get().isValid()) {
            return Optional.empty();
        }

        Session session = sessionOpt.get();
        session.updateActivity();
        sessionRepository.save(session);

        return Optional.of(session);
    }

    // Private helper methods

    private AuthResponse createMfaChallenge(UserInfo user, String ipAddress, 
                                             String userAgent, String deviceFingerprint) {
        // Create partial session for MFA verification
        Session pendingSession = Session.builder()
                .sessionId(generateSessionId())
                .userId(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .createdAt(Instant.now())
                .lastActivityAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMinutes(5))) // Short expiry for MFA
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceFingerprint(deviceFingerprint)
                .mfaVerified(false)
                .mfaMethod(user.getMfaMethod())
                .build();

        // Generate MFA token
        String mfaToken = generateMfaToken();
        sessionRepository.saveMfaToken(mfaToken, pendingSession, Duration.ofMinutes(5));

        log.info("MFA challenge created for userId={}", user.getId());
        return AuthResponse.mfaRequired(mfaToken, user.getMfaMethod());
    }

    private TokenPair createSession(UserInfo user, String ipAddress, String userAgent,
                                     String deviceFingerprint, boolean mfaVerified) {
        // Enforce max sessions per user
        long existingSessions = sessionRepository.countByUserId(user.getId());
        if (existingSessions >= maxSessionsPerUser) {
            // Remove oldest session
            List<Session> sessions = sessionRepository.findByUserId(user.getId());
            if (!sessions.isEmpty()) {
                Session oldest = sessions.get(sessions.size() - 1);
                sessionRepository.delete(oldest.getSessionId());
            }
        }

        // Create session
        String sessionId = generateSessionId();
        Session session = Session.builder()
                .sessionId(sessionId)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .createdAt(Instant.now())
                .lastActivityAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofHours(sessionDurationHours)))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceFingerprint(deviceFingerprint)
                .mfaVerified(mfaVerified)
                .mfaMethod(user.getMfaMethod())
                .refreshCount(0)
                .build();

        // Generate tokens
        TokenPair tokenPair = generateTokens(session, user);

        // Store refresh token hash
        session.setRefreshTokenHash(hashToken(tokenPair.getRefreshToken()));

        // Save session
        sessionRepository.save(session);

        eventPublisher.publish(DomainEvent.authEvent(
                EventTypes.SESSION_CREATED,
                user.getId().toString(),
                Map.of("sessionId", sessionId, "ip", ipAddress)
        ));

        return tokenPair;
    }

    private TokenPair generateTokens(Session session) {
        Optional<UserInfo> userOpt = userServiceClient.getUserAuthInfo(session.getUserId());
        return generateTokens(session, userOpt.orElse(null));
    }

    private TokenPair generateTokens(Session session, UserInfo user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.CLAIM_USER_ID, session.getUserId().toString());
        claims.put(SecurityConstants.CLAIM_EMAIL, session.getEmail());
        claims.put(SecurityConstants.CLAIM_ROLES, session.getRoles());
        claims.put(SecurityConstants.CLAIM_SESSION_ID, session.getSessionId());
        claims.put(SecurityConstants.CLAIM_MFA_VERIFIED, session.isMfaVerified());

        String accessToken = jwtTokenProvider.generateAccessToken(
                session.getUserId().toString(), claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                session.getUserId().toString(), session.getSessionId());

        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidityMs() / 1000)
                .refreshExpiresIn(jwtTokenProvider.getRefreshTokenValidityMs() / 1000)
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .roles(session.getRoles())
                .issuedAt(Instant.now())
                .mfaRequired(!session.isMfaVerified())
                .mfaMethod(session.getMfaMethod())
                .build();
    }

    private String generateSessionId() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return "sess_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateMfaToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "mfa_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
