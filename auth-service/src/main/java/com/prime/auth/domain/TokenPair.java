package com.prime.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Token pair response containing access and refresh tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenPair {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn; // seconds
    private long refreshExpiresIn; // seconds
    private String sessionId;
    private UUID userId;
    private Set<String> roles;
    private Instant issuedAt;
    
    private boolean mfaRequired;
    private String mfaMethod;
}
