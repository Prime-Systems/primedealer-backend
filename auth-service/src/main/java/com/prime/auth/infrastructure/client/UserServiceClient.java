package com.prime.auth.infrastructure.client;

import com.prime.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Client for communicating with User Service.
 * Handles service-to-service communication for authentication operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${user-service.url:http://user-service:8081}")
    private String userServiceUrl;

    @Value("${user-service.timeout-ms:5000}")
    private long timeoutMs;

    /**
     * Validate user credentials.
     */
    public Optional<UserInfo> validateCredentials(String email, String password, String ipAddress) {
        log.debug("Validating credentials for email: {}", email);

        try {
            WebClient client = webClientBuilder.baseUrl(userServiceUrl).build();

            ApiResponse<UserInfo> response = client.post()
                    .uri("/internal/v1/users/validate-credentials")
                    .bodyValue(Map.of(
                            "email", email,
                            "password", password,
                            "ipAddress", ipAddress
                    ))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<UserInfo>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response != null && response.isSuccess() && response.getData() != null) {
                return Optional.of(response.getData());
            }

            log.debug("Credential validation failed for email: {}", email);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error validating credentials: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Verify MFA code for a user.
     */
    public boolean verifyMfa(UUID userId, String code) {
        log.debug("Verifying MFA for user: {}", userId);

        try {
            WebClient client = webClientBuilder.baseUrl(userServiceUrl).build();

            ApiResponse<Boolean> response = client.post()
                    .uri("/internal/v1/users/{userId}/verify-mfa", userId)
                    .bodyValue(Map.of("code", code))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Boolean>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());

        } catch (Exception e) {
            log.error("Error verifying MFA: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verify passkey assertion.
     */
    public Optional<UserInfo> verifyPasskey(String email, Map<String, Object> credential) {
        log.debug("Verifying passkey for email: {}", email);

        try {
            WebClient client = webClientBuilder.baseUrl(userServiceUrl).build();

            ApiResponse<UserInfo> response = client.post()
                    .uri("/internal/v1/users/verify-passkey")
                    .bodyValue(Map.of(
                            "email", email,
                            "credential", credential
                    ))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<UserInfo>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response != null && response.isSuccess() && response.getData() != null) {
                return Optional.of(response.getData());
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error verifying passkey: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Get user auth info for token generation.
     */
    public Optional<UserInfo> getUserAuthInfo(UUID userId) {
        log.debug("Getting auth info for user: {}", userId);

        try {
            WebClient client = webClientBuilder.baseUrl(userServiceUrl).build();

            ApiResponse<UserInfo> response = client.get()
                    .uri("/internal/v1/users/{userId}/auth-info", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<UserInfo>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response != null && response.isSuccess()) {
                return Optional.ofNullable(response.getData());
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error getting user auth info: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Get user MFA status.
     */
    public Map<String, Object> getMfaStatus(UUID userId) {
        log.debug("Getting MFA status for user: {}", userId);

        try {
            WebClient client = webClientBuilder.baseUrl(userServiceUrl).build();

            ApiResponse<Map<String, Object>> response = client.get()
                    .uri("/internal/v1/users/{userId}/mfa-status", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }

            return Map.of("mfaEnabled", false);

        } catch (Exception e) {
            log.error("Error getting MFA status: {}", e.getMessage(), e);
            return Map.of("mfaEnabled", false);
        }
    }

    /**
     * Process OAuth login/registration.
     */
    public Optional<UserInfo> processOAuth(OAuthRegistrationRequest request) {
        log.debug("Processing OAuth for email: {}", request.getEmail());

        try {
            WebClient client = webClientBuilder.baseUrl(userServiceUrl).build();

            ApiResponse<UserInfo> response = client.post()
                    .uri("/internal/v1/users/oauth-process")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<UserInfo>>() {})
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();

            if (response != null && response.isSuccess() && response.getData() != null) {
                return Optional.of(response.getData());
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error processing OAuth: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
