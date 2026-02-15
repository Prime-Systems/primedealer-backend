package com.prime.user.api;

import com.prime.common.dto.ApiResponse;
import com.prime.user.application.dto.OAuthRegistrationRequest;
import com.prime.user.application.dto.UserResponse;
import com.prime.user.application.service.MfaService;
import com.prime.user.application.service.PasskeyService;
import com.prime.user.application.service.UserService;
import com.prime.user.domain.entity.User;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal API controller for service-to-service communication.
 * These endpoints are not exposed through the API Gateway.
 */
@Slf4j
@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
@Hidden // Hide from OpenAPI documentation
public class InternalUserController {

    private final UserService userService;
    private final MfaService mfaService;
    private final PasskeyService passkeyService;

    /**
     * Validate user credentials for authentication.
     * Called by Auth Service during login.
     */
    @PostMapping("/validate-credentials")
    public ResponseEntity<ApiResponse<UserResponse>> validateCredentials(
            @RequestBody Map<String, String> request) {
        
        String email = request.get("email");
        String password = request.get("password");
        String ipAddress = request.get("ipAddress");

        log.debug("Validating credentials for email: {}", email);

        Optional<User> userOpt = userService.findUserEntityByEmail(email);
        
        if (userOpt.isEmpty()) {
            log.debug("User not found: {}", email);
            return ResponseEntity.ok(ApiResponse.error("AUTH_001", "Invalid credentials", null));
        }

        User user = userOpt.get();

        // Check if account is locked
        if (user.isLocked()) {
            log.debug("Account locked: {}", email);
            return ResponseEntity.ok(ApiResponse.error("AUTH_002", "Account is locked", null));
        }

        // Check if account is active
        if (!user.isActive()) {
            log.debug("Account not active: {}", email);
            return ResponseEntity.ok(ApiResponse.error("AUTH_003", "Account is not active", null));
        }

        // Verify password using PasswordEncoder
        org.springframework.security.crypto.password.PasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        
        if (!encoder.matches(password, user.getPasswordHash())) {
            userService.recordFailedLogin(email);
            log.debug("Invalid password for: {}", email);
            return ResponseEntity.ok(ApiResponse.error("AUTH_001", "Invalid credentials", null));
        }

        // Record successful login
        userService.recordSuccessfulLogin(user.getId(), ipAddress);

        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromEntity(user), null));
    }

    /**
     * Verify MFA code for a user.
     * Called by Auth Service during MFA verification.
     */
    @PostMapping("/{userId}/verify-mfa")
    public ResponseEntity<ApiResponse<Boolean>> verifyMfa(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> request) {
        
        String code = request.get("code");
        boolean verified = mfaService.verifyMfaCode(userId, code);
        
        return ResponseEntity.ok(ApiResponse.success(verified, null));
    }

    /**
     * Verify passkey assertion for authentication.
     * Called by Auth Service during passkey login.
     */
    @PostMapping("/verify-passkey")
    public ResponseEntity<ApiResponse<UserResponse>> verifyPasskey(
            @RequestBody Map<String, Object> request) {
        
        String email = (String) request.get("email");
        @SuppressWarnings("unchecked")
        Map<String, Object> credential = (Map<String, Object>) request.get("credential");

        Optional<User> userOpt = passkeyService.verifyAuthentication(email, credential);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("AUTH_001", "Invalid passkey", null));
        }

        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromEntity(userOpt.get()), null));
    }

    /**
     * Get user for token generation.
     * Called by Auth Service when issuing tokens.
     */
    @GetMapping("/{userId}/auth-info")
    public ResponseEntity<ApiResponse<UserResponse>> getAuthInfo(@PathVariable UUID userId) {
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user, null));
    }

    /**
     * Check if user has MFA enabled.
     */
    @GetMapping("/{userId}/mfa-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMfaStatus(@PathVariable UUID userId) {
        UserResponse user = userService.getUserById(userId);
        Map<String, Object> status = Map.of(
                "mfaEnabled", user.isMfaEnabled(),
                "mfaMethod", user.getMfaMethod() != null ? user.getMfaMethod() : ""
        );
        return ResponseEntity.ok(ApiResponse.success(status, null));
    }

    /**
     * Process OAuth login/registration.
     * Called by Auth Service after successful OAuth provider callback.
     */
    @PostMapping("/oauth-process")
    public ResponseEntity<ApiResponse<UserResponse>> processOAuth(
            @RequestBody OAuthRegistrationRequest request) {
        
        log.info("Processing OAuth request for email: {}", request.getEmail());
        UserResponse user = userService.processOAuthUser(request);
        return ResponseEntity.ok(ApiResponse.success(user, "OAuth processed successfully"));
    }
}
