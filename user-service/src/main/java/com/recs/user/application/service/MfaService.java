package com.prime.user.application.service;

import com.prime.common.event.DomainEvent;
import com.prime.common.event.EventPublisher;
import com.prime.common.event.EventTypes;
import com.prime.common.exception.AuthenticationException;
import com.prime.common.exception.ResourceNotFoundException;
import com.prime.user.application.dto.MfaSetupResponse;
import com.prime.user.application.dto.MfaVerifyRequest;
import com.prime.user.domain.entity.MfaBackupCode;
import com.prime.user.domain.entity.OneTimeToken;
import com.prime.user.domain.entity.User;
import com.prime.user.domain.repository.MfaBackupCodeRepository;
import com.prime.user.domain.repository.OneTimeTokenRepository;
import com.prime.user.domain.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * MFA service handling TOTP setup, verification, and backup codes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private final UserRepository userRepository;
    private final MfaBackupCodeRepository backupCodeRepository;
    private final OneTimeTokenRepository oneTimeTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    @Value("${app.name:RECS Platform}")
    private String appName;

    private static final int BACKUP_CODE_COUNT = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(HashingAlgorithm.SHA1),
            new SystemTimeProvider()
    );

    /**
     * Initiate MFA setup for a user.
     */
    @Transactional
    public MfaSetupResponse initiateMfaSetup(UUID userId) {
        log.info("Initiating MFA setup for user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString()));

        if (user.isMfaEnabled()) {
            throw new IllegalStateException("MFA is already enabled for this user");
        }

        // Generate secret
        String secret = secretGenerator.generate();

        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();

        // Store backup codes (hashed)
        backupCodeRepository.deleteByUserId(userId);
        for (String code : backupCodes) {
            MfaBackupCode backupCode = MfaBackupCode.builder()
                    .user(user)
                    .codeHash(passwordEncoder.encode(code))
                    .build();
            backupCodeRepository.save(backupCode);
        }

        // Create setup token (temporary, for verification)
        String setupToken = generateSetupToken();
        OneTimeToken token = OneTimeToken.builder()
                .user(user)
                .tokenHash(hashToken(setupToken))
                .tokenType(OneTimeToken.TokenType.MFA_SETUP)
                .expiresAt(Instant.now().plusSeconds(600)) // 10 minutes
                .build();
        oneTimeTokenRepository.save(token);

        // Temporarily store secret in Redis or similar (not shown here)
        // For simplicity, we'll store it encrypted in the token metadata

        // Generate QR code URI
        QrData qrData = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(appName)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrCodeUri = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                appName, user.getEmail(), secret, appName
        );

        // Store the secret temporarily (would use Redis in production)
        user.setMfaSecret(secret); // Temporarily set, will be confirmed on verification
        userRepository.save(user);

        log.info("MFA setup initiated for user: userId={}", userId);

        return MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeUri(qrCodeUri)
                .backupCodes(backupCodes)
                .setupToken(setupToken)
                .build();
    }

    /**
     * Complete MFA setup by verifying the first code.
     */
    @Transactional
    public void completeMfaSetup(UUID userId, MfaVerifyRequest request) {
        log.info("Completing MFA setup for user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString()));

        // Verify setup token
        String tokenHash = hashToken(request.getSetupToken());
        OneTimeToken setupToken = oneTimeTokenRepository
                .findByTokenHashAndTokenTypeAndUsedFalseAndExpiresAtAfter(
                        tokenHash, OneTimeToken.TokenType.MFA_SETUP, Instant.now())
                .orElseThrow(() -> AuthenticationException.tokenInvalid());

        // Verify the TOTP code
        if (!verifyCode(user.getMfaSecret(), request.getCode())) {
            throw AuthenticationException.mfaInvalid();
        }

        // Enable MFA
        user.enableMfa(user.getMfaSecret());
        userRepository.save(user);

        // Invalidate setup token
        setupToken.markUsed();
        oneTimeTokenRepository.save(setupToken);

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.MFA_ENABLED,
                userId.toString(),
                Map.of("method", "TOTP")
        ));

        log.info("MFA setup completed for user: userId={}", userId);
    }

    /**
     * Verify MFA code.
     */
    public boolean verifyMfaCode(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString()));

        if (!user.isMfaEnabled()) {
            return true; // MFA not enabled, consider verified
        }

        // Try TOTP first
        if (verifyCode(user.getMfaSecret(), code)) {
            eventPublisher.publish(DomainEvent.userEvent(
                    EventTypes.MFA_VERIFIED,
                    userId.toString(),
                    Map.of("method", "TOTP")
            ));
            return true;
        }

        // Try backup code
        return verifyBackupCode(userId, code);
    }

    /**
     * Verify backup code.
     */
    @Transactional
    public boolean verifyBackupCode(UUID userId, String code) {
        List<MfaBackupCode> backupCodes = backupCodeRepository.findByUserIdAndUsedFalse(userId);

        for (MfaBackupCode backupCode : backupCodes) {
            if (passwordEncoder.matches(code, backupCode.getCodeHash())) {
                backupCode.markUsed();
                backupCodeRepository.save(backupCode);

                eventPublisher.publish(DomainEvent.userEvent(
                        EventTypes.MFA_BACKUP_CODE_USED,
                        userId.toString(),
                        Map.of("remainingCodes", backupCodes.size() - 1)
                ));

                log.info("Backup code used for user: userId={}, remaining={}",
                        userId, backupCodes.size() - 1);
                return true;
            }
        }

        return false;
    }

    /**
     * Disable MFA for a user.
     */
    @Transactional
    public void disableMfa(UUID userId, String password) {
        log.info("Disabling MFA for user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString()));

        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw AuthenticationException.invalidCredentials();
        }

        user.disableMfa();
        userRepository.save(user);

        // Delete backup codes
        backupCodeRepository.deleteByUserId(userId);

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.MFA_DISABLED,
                userId.toString(),
                Map.of()
        ));

        log.info("MFA disabled for user: userId={}", userId);
    }

    /**
     * Regenerate backup codes.
     */
    @Transactional
    public List<String> regenerateBackupCodes(UUID userId) {
        log.info("Regenerating backup codes for user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString()));

        if (!user.isMfaEnabled()) {
            throw new IllegalStateException("MFA is not enabled");
        }

        // Delete old codes
        backupCodeRepository.deleteByUserId(userId);

        // Generate new codes
        List<String> backupCodes = generateBackupCodes();
        for (String code : backupCodes) {
            MfaBackupCode backupCode = MfaBackupCode.builder()
                    .user(user)
                    .codeHash(passwordEncoder.encode(code))
                    .build();
            backupCodeRepository.save(backupCode);
        }

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.MFA_BACKUP_CODES_GENERATED,
                userId.toString(),
                Map.of("count", backupCodes.size())
        ));

        return backupCodes;
    }

    /**
     * Get remaining backup codes count.
     */
    @Transactional(readOnly = true)
    public long getRemainingBackupCodesCount(UUID userId) {
        return backupCodeRepository.countByUserIdAndUsedFalse(userId);
    }

    // Private helper methods

    private boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            codes.add(generateBackupCode());
        }
        return codes;
    }

    private String generateBackupCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }
        return code.substring(0, 4) + "-" + code.substring(4, 8);
    }

    private String generateSetupToken() {
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
}
