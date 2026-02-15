package com.prime.auth.api.controller;

import com.prime.auth.application.dto.*;
import com.prime.auth.application.service.AuthenticationService;
import com.prime.common.dto.ApiResponse;
import com.prime.common.security.RequestContextUtil;
import com.prime.common.security.SecurityConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication REST API controller.
 * Handles login, token management, and session operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and session management APIs")
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Login with email and password.
     */
    @PostMapping("/login")
    @Operation(summary = "Login with credentials", 
            description = "Authenticate with email and password. Returns tokens or MFA challenge.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = RequestContextUtil.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authenticationService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Login with passkey.
     */
    @PostMapping("/login/passkey")
    @Operation(summary = "Login with passkey", 
            description = "Authenticate using WebAuthn/FIDO2 passkey")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithPasskey(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        String email = (String) request.get("email");
        @SuppressWarnings("unchecked")
        Map<String, Object> credential = (Map<String, Object>) request.get("credential");

        String ipAddress = RequestContextUtil.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authenticationService.loginWithPasskey(
                email, credential, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Complete MFA verification.
     */
    @PostMapping("/mfa/verify")
    @Operation(summary = "Verify MFA", 
            description = "Complete MFA verification and receive tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfa(
            @Valid @RequestBody MfaVerificationRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = RequestContextUtil.getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authenticationService.verifyMfa(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Refresh access token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", 
            description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Logout current session.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke current session")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        String sessionId = httpRequest.getHeader(SecurityConstants.HEADER_SESSION_ID);
        if (sessionId != null) {
            authenticationService.logout(sessionId);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    /**
     * Logout from all sessions.
     */
    @PostMapping("/logout/all")
    @Operation(summary = "Logout all", description = "Revoke all sessions for current user")
    public ResponseEntity<ApiResponse<Void>> logoutAll(HttpServletRequest httpRequest) {
        String userIdStr = httpRequest.getHeader(SecurityConstants.HEADER_USER_ID);
        if (userIdStr != null) {
            UUID userId = UUID.fromString(userIdStr);
            authenticationService.logoutAll(userId);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "All sessions revoked"));
    }

    /**
     * Get active sessions.
     */
    @GetMapping("/sessions")
    @Operation(summary = "List sessions", description = "Get all active sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            HttpServletRequest httpRequest) {
        System.out.println("Getting sessions...");
        System.out.println("Headers: " + httpRequest.getHeaderNames());
        System.out.println("User ID Header: " + httpRequest.getHeader(SecurityConstants.HEADER_USER_ID));

        String userIdStr = httpRequest.getHeader(SecurityConstants.HEADER_USER_ID);
        System.out.println("User ID from header: " + userIdStr);
        String currentSessionId = httpRequest.getHeader(SecurityConstants.HEADER_SESSION_ID);
        System.out.println("Current Session ID from header: " + currentSessionId);

        if (userIdStr == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        UUID userId = UUID.fromString(userIdStr);
        List<SessionResponse> sessions = authenticationService.getSessions(userId, currentSessionId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Revoke a specific session.
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke session", description = "Revoke a specific session")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {

        String userIdStr = httpRequest.getHeader(SecurityConstants.HEADER_USER_ID);
        if (userIdStr != null) {
            UUID userId = UUID.fromString(userIdStr);
            authenticationService.revokeSession(userId, sessionId);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Session revoked"));
    }

    /**
     * Validate token (internal endpoint for API Gateway).
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate token", description = "Validate access token (internal use)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestBody Map<String, String> request) {

        String accessToken = request.get("accessToken");
        var sessionOpt = authenticationService.validateToken(accessToken);

        if (sessionOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Invalid token"));
        }

        var session = sessionOpt.get();
        Map<String, Object> result = Map.of(
                "valid", true,
                "userId", session.getUserId().toString(),
                "email", session.getEmail(),
                "roles", session.getRoles(),
                "sessionId", session.getSessionId(),
                "mfaVerified", session.isMfaVerified()
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
