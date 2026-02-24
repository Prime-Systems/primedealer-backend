package com.prime.user.application.service;

import com.prime.common.event.DomainEvent;
import com.prime.common.event.EventPublisher;
import com.prime.common.event.EventTypes;
import com.prime.common.exception.ResourceNotFoundException;
import com.prime.common.exception.ValidationException;
import com.prime.common.security.UserRole;
import com.prime.user.application.dto.*;
import com.prime.user.domain.entity.OAuthProvider;
import com.prime.user.domain.entity.OneTimeToken;
import com.prime.user.domain.entity.User;
import com.prime.user.domain.repository.OneTimeTokenRepository;
import com.prime.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * User service handling user registration, profile management, and credential operations.
 * Follows clean architecture with domain logic in entities and orchestration in services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OneTimeTokenRepository oneTimeTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Register a new user.
     */
    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering new user: email={}", request.getEmail());

        // Validate uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ValidationException.duplicateEmail(request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw ValidationException.duplicateUsername(request.getUsername());
        }
        if (userRepository.existsByGhanaCardNumber(request.getGhanaCardNumber())) {
            throw ValidationException.duplicateGhanaCardNumber(request.getGhanaCardNumber());
        }

        // Validate password strength
        validatePasswordStrength(request.getPassword());

        // Create user entity
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .username(request.getUsername().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .status(User.UserStatus.PENDING_VERIFICATION)
                .roles(new HashSet<>(Set.of(request.getRole())))
                .ghanaCardNumber(request.getGhanaCardNumber())
                .build();

        user = userRepository.save(user);

        // Generate email verification token
        createVerificationToken(user);

        // Publish event
        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.USER_REGISTERED,
                user.getId().toString(),
                Map.of(
                        "email", user.getEmail(),
                        "username", user.getUsername()
                )
        ));

        log.info("User registered successfully: userId={}", user.getId());
        return UserResponse.fromEntity(user);
    }

    /**
     * Get user by ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString()));
        return UserResponse.fromEntity(user);
    }

    /**
     * Get user by email.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> ResourceNotFoundException.user(email));
        return UserResponse.fromEntity(user);
    }

    /**
     * Find or create user from OAuth info.
     */
    @Transactional
    public UserResponse processOAuthUser(OAuthRegistrationRequest request) {
        log.info("Processing OAuth user: provider={}, providerUserId={}, email={}", 
                request.getProvider(), request.getProviderUserId(), request.getEmail());

        // 1. Check if OAuth provider already linked
        Optional<User> userByOAuth = userRepository.findByOAuthProvider(
                request.getProvider(), request.getProviderUserId());
        
        if (userByOAuth.isPresent()) {
            log.debug("Found existing user by OAuth provider");
            return UserResponse.fromEntity(userByOAuth.get());
        }

        // 2. Check if user exists by email
        Optional<User> userByEmail = userRepository.findByEmail(request.getEmail().toLowerCase());
        
        if (userByEmail.isPresent()) {
            log.info("Linking OAuth provider to existing user by email: {}", request.getEmail());
            User user = userByEmail.get();
            linkOAuthProvider(user, request);
            return UserResponse.fromEntity(user);
        }

        // 3. Create new user
        log.info("Creating new user from OAuth: {}", request.getEmail());
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .username(generateUniqueUsername(request.getEmail()))
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true) // OAuth emails are pre-verified
                .roles(new HashSet<>(Set.of(request.getRole() != null ? request.getRole() : UserRole.BUYER)))
                .build();

        user = userRepository.save(user);
        linkOAuthProvider(user, request);

        // Publish registration event
        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.USER_REGISTERED,
                user.getId().toString(),
                Map.of(
                        "email", user.getEmail(),
                        "username", user.getUsername(),
                        "registrationType", "OAUTH",
                        "provider", request.getProvider()
                )
        ));

        return UserResponse.fromEntity(user);
    }

    private void linkOAuthProvider(User user, OAuthRegistrationRequest request) {
        OAuthProvider oauthProvider = OAuthProvider.builder()
                .user(user)
                .provider(request.getProvider())
                .providerUserId(request.getProviderUserId())
                .email(request.getEmail())
                .build();
        
        user.getOauthProviders().add(oauthProvider);
        userRepository.save(user);
    }

    private String generateUniqueUsername(String email) {
        String base = email.split("@")[0];
        // Ensure username format is valid
        base = base.replaceAll("[^a-zA-Z0-9_-]", "");
        if (base.length() < 3) base = base + "user";
        if (base.length() > 40) base = base.substring(0, 40);
        
        String username = base;
        int count = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + count++;
        }
        return username;
    }

    /**
     * Get user entity by email (for internal authentication use).
     */
    @Transactional(readOnly = true)
    public Optional<User> findUserEntityByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    /**
     * Update user profile.
     */
    @Transactional
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        log.info("Updating user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString()));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        user = userRepository.save(user);

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.USER_UPDATED,
                userId.toString(),
                Map.of("updatedFields", getUpdatedFields(request))
        ));

        return UserResponse.fromEntity(user);
    }

    /**
     * Change user password.
     */
    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        log.info("Changing password for user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString()));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect", 
                    ValidationException.VALIDATION_FAILED,
                    Map.of("currentPassword", "Incorrect password"));
        }

        // Validate new password
        validatePasswordStrength(request.getNewPassword());

        // Ensure new password is different
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new ValidationException("New password must be different from current password",
                    ValidationException.VALIDATION_FAILED,
                    Map.of("newPassword", "Must be different from current password"));
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all password reset tokens
        oneTimeTokenRepository.invalidateUserTokensByType(
                userId, OneTimeToken.TokenType.PASSWORD_RESET, Instant.now());

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.PASSWORD_CHANGED,
                userId.toString(),
                Map.of()
        ));

        log.info("Password changed successfully for user: userId={}", userId);
    }

    /**
     * Verify email with token.
     */
    @Transactional
    public void verifyEmail(String token) {
        log.info("Verifying email with token");

        String tokenHash = hashToken(token);
        OneTimeToken oneTimeToken = oneTimeTokenRepository
                .findByTokenHashAndTokenTypeAndUsedFalseAndExpiresAtAfter(
                        tokenHash, OneTimeToken.TokenType.EMAIL_VERIFICATION, Instant.now())
                .orElseThrow(() -> ResourceNotFoundException.token("verification token"));

        User user = oneTimeToken.getUser();
        user.verifyEmail();
        userRepository.save(user);

        oneTimeToken.markUsed();
        oneTimeTokenRepository.save(oneTimeToken);

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.USER_VERIFIED,
                user.getId().toString(),
                Map.of("email", user.getEmail())
        ));

        log.info("Email verified successfully for user: userId={}", user.getId());
    }

    /**
     * Request password reset.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());
        
        // Always respond with success to prevent email enumeration
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();
        
        // Invalidate previous reset tokens
        oneTimeTokenRepository.invalidateUserTokensByType(
                user.getId(), OneTimeToken.TokenType.PASSWORD_RESET, Instant.now());

        // Generate new token
        String token = generateSecureToken();
        OneTimeToken resetToken = OneTimeToken.builder()
                .user(user)
                .tokenHash(hashToken(token))
                .tokenType(OneTimeToken.TokenType.PASSWORD_RESET)
                .expiresAt(Instant.now().plusSeconds(3600)) // 1 hour
                .build();
        oneTimeTokenRepository.save(resetToken);

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.PASSWORD_RESET_REQUESTED,
                user.getId().toString(),
                Map.of("token", token) // This would go to email service
        ));
    }

    /**
     * Reset password with token.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Resetting password with token");

        String tokenHash = hashToken(token);
        OneTimeToken resetToken = oneTimeTokenRepository
                .findByTokenHashAndTokenTypeAndUsedFalseAndExpiresAtAfter(
                        tokenHash, OneTimeToken.TokenType.PASSWORD_RESET, Instant.now())
                .orElseThrow(() -> ResourceNotFoundException.token("reset token"));

        validatePasswordStrength(newPassword);

        User user = resetToken.getUser();
        user.changePassword(passwordEncoder.encode(newPassword));
        user.unlock(); // Unlock if locked
        userRepository.save(user);

        resetToken.markUsed();
        oneTimeTokenRepository.save(resetToken);

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.PASSWORD_RESET_COMPLETED,
                user.getId().toString(),
                Map.of()
        ));

        log.info("Password reset successfully for user: userId={}", user.getId());
    }

    /**
     * Record a failed login attempt.
     */
    @Transactional
    public void recordFailedLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.recordFailedLogin();
            userRepository.save(user);

            if (user.isLocked()) {
                eventPublisher.publish(DomainEvent.userEvent(
                        EventTypes.USER_LOCKED,
                        user.getId().toString(),
                        Map.of("reason", "Too many failed login attempts")
                ));
            }
        });
    }

    /**
     * Record a successful login.
     */
    @Transactional
    public void recordSuccessfulLogin(UUID userId, String ipAddress) {
        userRepository.findById(userId).ifPresent(user -> {
            user.recordSuccessfulLogin(ipAddress);
            userRepository.save(user);
        });
    }

    @Transactional
    public void approveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User approved: userId={}", userId);
    }

    @Transactional
    public void banUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User",  userId.toString()));
        user.setStatus(User.UserStatus.SUSPENDED);
        userRepository.save(user);
        log.info("User banned: userId={}", userId);
    }

    // Private helper methods

    private void createVerificationToken(User user) {
        String token = generateSecureToken();
        System.out.println("token: " + token); // For testing purposes
        OneTimeToken verificationToken = OneTimeToken.builder()
                .user(user)
                .tokenHash(hashToken(token))
                .tokenType(OneTimeToken.TokenType.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().plusSeconds(86400)) // 24 hours
                .build();
        oneTimeTokenRepository.save(verificationToken);

        // In production, this would trigger an email
        log.info("Verification token generated for user: userId={}", user.getId());
    }

    private void validatePasswordStrength(String password) {
        List<String> issues = new ArrayList<>();

        if (password.length() < 12) {
            issues.add("at least 12 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            issues.add("an uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            issues.add("a lowercase letter");
        }
        if (!password.matches(".*[0-9].*")) {
            issues.add("a number");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            issues.add("a special character");
        }

        if (!issues.isEmpty()) {
            throw ValidationException.weakPassword(
                    "Password must contain: " + String.join(", ", issues));
        }
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
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

    private List<String> getUpdatedFields(UserUpdateRequest request) {
        List<String> fields = new ArrayList<>();
        if (request.getFirstName() != null) fields.add("firstName");
        if (request.getLastName() != null) fields.add("lastName");
        if (request.getPhoneNumber() != null) fields.add("phoneNumber");
        return fields;
    }
}
