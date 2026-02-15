package com.prime.user.application.service;

import com.prime.common.event.DomainEvent;
import com.prime.common.event.EventPublisher;
import com.prime.common.event.EventTypes;
import com.prime.common.exception.ResourceNotFoundException;
import com.prime.user.application.dto.PasskeyRegistrationResponse;
import com.prime.user.domain.entity.Passkey;
import com.prime.user.domain.entity.User;
import com.prime.user.domain.repository.PasskeyRepository;
import com.prime.user.domain.repository.UserRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Passkey service handling WebAuthn credential registration and authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasskeyService {

    private final PasskeyRepository passkeyRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final EventPublisher eventPublisher;
    private final WebAuthnManager webAuthnManager;

    @Value("${webauthn.rp.id:localhost}")
    private String rpId;

    @Value("${webauthn.rp.name:RECS Platform}")
    private String rpName;

    @Value("${webauthn.rp.origin:http://localhost:3000}")
    private String origin;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHALLENGE_PREFIX = "passkey:challenge:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    /**
     * Generate options for passkey registration.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateRegistrationOptions(UUID userId) {
        log.info("Generating passkey registration options for user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString()));

        // Get existing credentials to exclude
        List<Passkey> existingPasskeys = passkeyRepository.findByUserIdAndRevokedFalse(userId);
        List<Map<String, Object>> excludeCredentials = existingPasskeys.stream()
                .map(p -> Map.<String, Object>of(
                        "type", "public-key",
                        "id", p.getCredentialId(),
                        "transports", p.getTransports() != null ? 
                                Arrays.asList(p.getTransports().split(",")) : List.of()
                ))
                .collect(Collectors.toList());

        // Generate challenge
        byte[] challengeBytes = new byte[32];
        SECURE_RANDOM.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        // Store challenge in Redis
        String challengeKey = CHALLENGE_PREFIX + userId;
        redisTemplate.opsForValue().set(challengeKey, challenge, CHALLENGE_TTL);

        // Build registration options
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("challenge", challenge);
        options.put("rp", Map.of("id", rpId, "name", rpName));
        options.put("user", Map.of(
                "id", Base64.getUrlEncoder().withoutPadding().encodeToString(userId.toString().getBytes()),
                "name", user.getEmail(),
                "displayName", user.getFullName()
        ));
        options.put("pubKeyCredParams", List.of(
                Map.of("type", "public-key", "alg", -7),  // ES256
                Map.of("type", "public-key", "alg", -257) // RS256
        ));
        options.put("timeout", 60000);
        options.put("attestation", "none");
        options.put("excludeCredentials", excludeCredentials);
        options.put("authenticatorSelection", Map.of(
                "authenticatorAttachment", "platform",
                "residentKey", "preferred",
                "userVerification", "preferred"
        ));

        return options;
    }

    /**
     * Complete passkey registration.
     */
    @Transactional
    public PasskeyRegistrationResponse completeRegistration(UUID userId, 
                                                             Map<String, Object> credential,
                                                             String deviceName) {
        log.info("Completing passkey registration for user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.user(userId.toString()));

        // Retrieve and validate challenge
        String challengeKey = CHALLENGE_PREFIX + userId;
        String storedChallenge = redisTemplate.opsForValue().get(challengeKey);
        if (storedChallenge == null) {
            throw new IllegalStateException("Registration session expired");
        }
        redisTemplate.delete(challengeKey);

        // Extract credential data
        String credentialId = (String) credential.get("id");
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) credential.get("response");
        String clientDataJSON = (String) response.get("clientDataJSON");
        String attestationObject = (String) response.get("attestationObject");

        // In production, use WebAuthn4J to validate the attestation
        // For now, we'll store the credential directly
        byte[] publicKeyCose = Base64.getUrlDecoder().decode((String) response.get("publicKey"));
        
        // Check for duplicate
        if (passkeyRepository.existsByCredentialId(credentialId)) {
            throw new IllegalStateException("Credential already registered");
        }

        // Extract transports
        @SuppressWarnings("unchecked")
        List<String> transports = (List<String>) credential.get("transports");

        // Create passkey entity
        Passkey passkey = Passkey.builder()
                .user(user)
                .credentialId(credentialId)
                .publicKeyCose(publicKeyCose)
                .signCount(0)
                .deviceName(deviceName != null ? deviceName : "Passkey")
                .transports(transports != null ? String.join(",", transports) : null)
                .attestationFormat("none")
                .userVerified(true)
                .build();

        passkey = passkeyRepository.save(passkey);

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.PASSKEY_REGISTERED,
                userId.toString(),
                Map.of(
                        "passkeyId", passkey.getId().toString(),
                        "deviceName", passkey.getDeviceName()
                )
        ));

        log.info("Passkey registered successfully: userId={}, passkeyId={}", 
                userId, passkey.getId());

        return PasskeyRegistrationResponse.builder()
                .id(passkey.getId())
                .credentialId(credentialId)
                .deviceName(passkey.getDeviceName())
                .createdAt(passkey.getCreatedAt())
                .build();
    }

    /**
     * Generate options for passkey authentication.
     */
    public Map<String, Object> generateAuthenticationOptions(String email) {
        log.info("Generating passkey authentication options for email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        
        List<Map<String, Object>> allowCredentials = new ArrayList<>();
        if (userOpt.isPresent()) {
            List<Passkey> passkeys = passkeyRepository.findByUserIdAndRevokedFalse(userOpt.get().getId());
            allowCredentials = passkeys.stream()
                    .map(p -> Map.<String, Object>of(
                            "type", "public-key",
                            "id", p.getCredentialId(),
                            "transports", p.getTransports() != null ? 
                                    Arrays.asList(p.getTransports().split(",")) : List.of()
                    ))
                    .collect(Collectors.toList());
        }

        // Generate challenge
        byte[] challengeBytes = new byte[32];
        SECURE_RANDOM.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        // Store challenge with email association
        if (userOpt.isPresent()) {
            String challengeKey = CHALLENGE_PREFIX + "auth:" + email;
            redisTemplate.opsForValue().set(challengeKey, challenge, CHALLENGE_TTL);
        }

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("challenge", challenge);
        options.put("rpId", rpId);
        options.put("timeout", 60000);
        options.put("userVerification", "preferred");
        options.put("allowCredentials", allowCredentials);

        return options;
    }

    /**
     * Verify passkey authentication.
     */
    @Transactional
    public Optional<User> verifyAuthentication(String email, Map<String, Object> credential) {
        log.info("Verifying passkey authentication for email: {}", email);

        // Retrieve and validate challenge
        String challengeKey = CHALLENGE_PREFIX + "auth:" + email;
        String storedChallenge = redisTemplate.opsForValue().get(challengeKey);
        if (storedChallenge == null) {
            log.warn("Authentication session expired for email: {}", email);
            return Optional.empty();
        }
        redisTemplate.delete(challengeKey);

        // Extract credential data
        String credentialId = (String) credential.get("id");

        // Find passkey
        Optional<Passkey> passkeyOpt = passkeyRepository.findByCredentialIdAndRevokedFalse(credentialId);
        if (passkeyOpt.isEmpty()) {
            log.warn("Passkey not found for credential: {}", credentialId);
            return Optional.empty();
        }

        Passkey passkey = passkeyOpt.get();
        User user = passkey.getUser();

        // Verify email matches
        if (!user.getEmail().equalsIgnoreCase(email)) {
            log.warn("Email mismatch for passkey authentication");
            return Optional.empty();
        }

        // In production, fully validate the assertion with WebAuthn4J
        // For now, we'll update sign count and return success
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) credential.get("response");
        
        // Update sign count (simplified - in production, extract from authenticator data)
        passkey.updateSignCount(passkey.getSignCount() + 1);
        passkeyRepository.save(passkey);

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.PASSKEY_USED,
                user.getId().toString(),
                Map.of("passkeyId", passkey.getId().toString())
        ));

        log.info("Passkey authentication successful: userId={}", user.getId());
        return Optional.of(user);
    }

    /**
     * List user's passkeys.
     */
    @Transactional(readOnly = true)
    public List<PasskeyRegistrationResponse> listPasskeys(UUID userId) {
        return passkeyRepository.findActivePasskeysByUserId(userId).stream()
                .map(p -> PasskeyRegistrationResponse.builder()
                        .id(p.getId())
                        .credentialId(p.getCredentialId())
                        .deviceName(p.getDeviceName())
                        .lastUsedAt(p.getLastUsedAt())
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Revoke a passkey.
     */
    @Transactional
    public void revokePasskey(UUID userId, UUID passkeyId) {
        log.info("Revoking passkey: userId={}, passkeyId={}", userId, passkeyId);

        Passkey passkey = passkeyRepository.findById(passkeyId)
                .orElseThrow(() -> ResourceNotFoundException.passkey(passkeyId.toString()));

        if (!passkey.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Passkey does not belong to user");
        }

        passkey.revoke();
        passkeyRepository.save(passkey);

        eventPublisher.publish(DomainEvent.userEvent(
                EventTypes.PASSKEY_REMOVED,
                userId.toString(),
                Map.of("passkeyId", passkeyId.toString())
        ));

        log.info("Passkey revoked: userId={}, passkeyId={}", userId, passkeyId);
    }
}
