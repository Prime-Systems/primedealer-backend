package com.prime.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.prime.common.security.UserRole;
import com.prime.auth.domain.TokenPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Authentication response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String sessionId;
    private UUID userId;
    private Set<UserRole> roles;
    
    // MFA-related fields
    private boolean mfaRequired;
    private String mfaMethod;
    private String mfaToken; // Temporary token for MFA verification

    /**
     * Create from TokenPair.
     */
    public static AuthResponse fromTokenPair(TokenPair tokenPair) {
        return AuthResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .refreshToken(tokenPair.getRefreshToken())
                .tokenType(tokenPair.getTokenType())
                .expiresIn(tokenPair.getExpiresIn())
                .sessionId(tokenPair.getSessionId())
                .userId(tokenPair.getUserId())
                .roles(tokenPair.getRoles())
                .mfaRequired(tokenPair.isMfaRequired())
                .mfaMethod(tokenPair.getMfaMethod())
                .build();
    }

    /**
     * Create MFA required response.
     */
    public static AuthResponse mfaRequired(String mfaToken, String mfaMethod) {
        return AuthResponse.builder()
                .mfaRequired(true)
                .mfaMethod(mfaMethod)
                .mfaToken(mfaToken)
                .build();
    }
}
