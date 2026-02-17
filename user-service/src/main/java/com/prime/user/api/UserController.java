package com.prime.user.api;

import com.prime.common.dto.ApiResponse;
import com.prime.common.security.SecurityConstants;
import com.prime.common.util.RequestContextUtil;
import com.prime.user.application.dto.*;
import com.prime.user.application.service.MfaService;
import com.prime.user.application.service.PasskeyService;
import com.prime.user.application.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * User API controller following API-first design principles.
 * Provides endpoints for user management, MFA, and passkey operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User identity and profile management APIs")
public class UserController {

    private final UserService userService;
    private final MfaService mfaService;
    private final PasskeyService passkeyService;

    // ==================== User Registration & Profile ====================

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        log.info("User registration request: email={}, requestId={}", 
                request.getEmail(), requestId);

        UserResponse user = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, requestId));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user, requestId));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
        UserResponse updatedUser = userService.updateUser(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, requestId));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID (admin only)")
    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user, requestId));
    }

    @PostMapping("/{userId}/approve")
    @Operation(summary = "Approve user (admin only)")
    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<Void>> approveUser(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        userService.approveUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, requestId));
    }

    @PostMapping("/{userId}/ban")
    @Operation(summary = "Ban user (admin only)")
    @PreAuthorize("hasRole('" + SecurityConstants.ROLE_ADMIN + "')")
    public ResponseEntity<ApiResponse<Void>> banUser(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        userService.banUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, requestId));
    }

    // ==================== Password Management ====================

    @PostMapping("/me/password")
    @Operation(summary = "Change current user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, requestId));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        String email = request.get("email");
        userService.requestPasswordReset(email);
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(ApiResponse.success(null, requestId));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        String token = request.get("token");
        String newPassword = request.get("password");
        userService.resetPassword(token, newPassword);
        return ResponseEntity.ok(ApiResponse.success(null, requestId));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        String token = request.get("token");
        userService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, requestId));
    }

    // ==================== MFA Management ====================

    @PostMapping("/me/mfa/setup")
    @Operation(summary = "Initiate MFA setup")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> initiateMfaSetup(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
        MfaSetupResponse response = mfaService.initiateMfaSetup(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, requestId));
    }

    @PostMapping("/me/mfa/verify")
    @Operation(summary = "Complete MFA setup by verifying first code")
    public ResponseEntity<ApiResponse<Void>> completeMfaSetup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
        mfaService.completeMfaSetup(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, requestId));
    }

    @DeleteMapping("/me/mfa")
    @Operation(summary = "Disable MFA")
    public ResponseEntity<ApiResponse<Void>> disableMfa(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
        mfaService.disableMfa(currentUser.getId(), request.get("password"));
        return ResponseEntity.ok(ApiResponse.success(null, requestId));
    }

    @PostMapping("/me/mfa/backup-codes")
    @Operation(summary = "Regenerate MFA backup codes")
    public ResponseEntity<ApiResponse<List<String>>> regenerateBackupCodes(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
        List<String> codes = mfaService.regenerateBackupCodes(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(codes, requestId));
    }

    // ==================== Passkey Management ====================

    @GetMapping("/me/passkeys")
    @Operation(summary = "List user's passkeys")
    public ResponseEntity<ApiResponse<List<PasskeyRegistrationResponse>>> listPasskeys(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
        List<PasskeyRegistrationResponse> passkeys = passkeyService.listPasskeys(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(passkeys, requestId));
    }

    @PostMapping("/me/passkeys/register/options")
    @Operation(summary = "Get passkey registration options")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPasskeyRegistrationOptions(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
        Map<String, Object> options = passkeyService.generateRegistrationOptions(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(options, requestId));
    }

    @PostMapping("/me/passkeys/register")
    @Operation(summary = "Complete passkey registration")
    public ResponseEntity<ApiResponse<PasskeyRegistrationResponse>> registerPasskey(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> credential = (Map<String, Object>) request.get("credential");
        String deviceName = (String) request.get("deviceName");
        
        PasskeyRegistrationResponse response = passkeyService.completeRegistration(
                currentUser.getId(), credential, deviceName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, requestId));
    }

    @DeleteMapping("/me/passkeys/{passkeyId}")
    @Operation(summary = "Revoke a passkey")
    public ResponseEntity<ApiResponse<Void>> revokePasskey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID passkeyId,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        UserResponse currentUser = userService.getUserByEmail(userDetails.getUsername());
        passkeyService.revokePasskey(currentUser.getId(), passkeyId);
        return ResponseEntity.ok(ApiResponse.success(null, requestId));
    }

    // ==================== Public Passkey Authentication Endpoints ====================

    @PostMapping("/passkey/assertion/options")
    @Operation(summary = "Get passkey authentication options")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPasskeyAuthOptions(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        String requestId = RequestContextUtil.getRequestId(httpRequest);
        String email = request.get("email");
        Map<String, Object> options = passkeyService.generateAuthenticationOptions(email);
        return ResponseEntity.ok(ApiResponse.success(options, requestId));
    }
}
