package com.prime.gateway.filter;

import com.prime.common.security.JwtTokenProvider;
import com.prime.common.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Global authentication filter for API Gateway.
 * Validates JWT tokens and enriches requests with user context.
 */
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;

    // Paths that don't require authentication
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/mfa/verify",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/users/register",
            "/api/v1/users/forgot-password",
            "/api/v1/users/reset-password",
            "/api/v1/users/verify-email",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars/swagger-ui",
            "/swagger-resources"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Missing or invalid Authorization header for path: {}", path);
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        // Validate token
        Optional<Claims> claimsOpt = jwtTokenProvider.validateAccessToken(token);

        if (claimsOpt.isEmpty()) {
            log.debug("Invalid token for path: {}", path);
            return unauthorized(exchange, "Invalid or expired token");
        }

        Claims claims = claimsOpt.get();

        // Check if MFA is required but not verified
        Boolean mfaVerified = claims.get(SecurityConstants.CLAIM_MFA_VERIFIED, Boolean.class);
        if (mfaVerified != null && !mfaVerified && requiresMfaVerification(path)) {
            return forbidden(exchange, "MFA verification required");
        }

        // Enrich request with user context
        ServerHttpRequest enrichedRequest = request.mutate()
                .header(SecurityConstants.HEADER_USER_ID, 
                        claims.get(SecurityConstants.CLAIM_USER_ID, String.class))
                .header(SecurityConstants.HEADER_USER_EMAIL, 
                        claims.get(SecurityConstants.CLAIM_EMAIL, String.class))
                .header(SecurityConstants.HEADER_SESSION_ID, 
                        claims.get(SecurityConstants.CLAIM_SESSION_ID, String.class))
                .header(SecurityConstants.HEADER_USER_ROLES, 
                        String.join(",", getRoles(claims)))
                .build();

        log.debug("Request authenticated: userId={}, path={}", 
                claims.get(SecurityConstants.CLAIM_USER_ID), path);

        return chain.filter(exchange.mutate().request(enrichedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100; // High priority - run before other filters
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean requiresMfaVerification(String path) {
        // Sensitive operations that require MFA
        return path.contains("/users/me/password") || 
               path.contains("/users/me/mfa") ||
               path.contains("/users/me/passkeys");
    }

    @SuppressWarnings("unchecked")
    private List<String> getRoles(Claims claims) {
        Object roles = claims.get(SecurityConstants.CLAIM_ROLES);
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.warn("Unauthorized request: {} - {}", exchange.getRequest().getPath(), message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("WWW-Authenticate", "Bearer");
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        log.warn("Forbidden request: {} - {}", exchange.getRequest().getPath(), message);
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}
