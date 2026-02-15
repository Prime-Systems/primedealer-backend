package com.prime.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * JWT Token Provider for token generation and validation.
 * Implements secure token handling with proper claims management.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey accessTokenKey;
    private final SecretKey refreshTokenKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${jwt.access-token.secret}") String accessTokenSecret,
            @Value("${jwt.refresh-token.secret}") String refreshTokenSecret,
            @Value("${jwt.access-token.validity-ms:900000}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token.validity-ms:604800000}") long refreshTokenValidityMs,
            @Value("${jwt.issuer:recs-platform}") String issuer) {
        
        this.accessTokenKey = Keys.hmacShaKeyFor(accessTokenSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenKey = Keys.hmacShaKeyFor(refreshTokenSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
        this.issuer = issuer;
    }

    /**
     * Generate an access token for the given subject with claims.
     */
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenValidityMs);

        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claims(claims)
                .claim("type", "access")
                .signWith(accessTokenKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Generate a refresh token for the given subject.
     */
    public String generateRefreshToken(String subject, String sessionId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(refreshTokenValidityMs);

        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("type", "refresh")
                .claim("sessionId", sessionId)
                .signWith(refreshTokenKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Validate and parse an access token.
     */
    public Optional<Claims> validateAccessToken(String token) {
        return validateToken(token, accessTokenKey, "access");
    }

    /**
     * Validate and parse a refresh token.
     */
    public Optional<Claims> validateRefreshToken(String token) {
        return validateToken(token, refreshTokenKey, "refresh");
    }

    private Optional<Claims> validateToken(String token, SecretKey key, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("type", String.class);
            if (!expectedType.equals(tokenType)) {
                log.warn("Invalid token type. Expected: {}, Got: {}", expectedType, tokenType);
                return Optional.empty();
            }

            return Optional.of(claims);
        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Extract subject from token without validation (for logging purposes).
     */
    public Optional<String> extractSubject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return Optional.empty();
            
            // Decode payload without verification
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            // Simple extraction - in production, use proper JSON parsing
            int subStart = payload.indexOf("\"sub\":\"") + 7;
            int subEnd = payload.indexOf("\"", subStart);
            return Optional.of(payload.substring(subStart, subEnd));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Get access token validity in milliseconds.
     */
    public long getAccessTokenValidityMs() {
        return accessTokenValidityMs;
    }

    /**
     * Get refresh token validity in milliseconds.
     */
    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }
}
