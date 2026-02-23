package com.prime.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Rate limiting configuration for Spring Cloud Gateway.
 * Defines key resolvers for rate limiting by client IP.
 */
@Configuration
public class RateLimitConfiguration {

    /**
     * Default key resolver that uses client IP address for rate limiting.
     * Falls back to "unknown" if IP cannot be determined.
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // Try X-Forwarded-For first (for clients behind proxy)
            String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                return Mono.just(xff.split(",")[0].trim());
            }

            // Fall back to remote address
            if (exchange.getRequest().getRemoteAddress() != null) {
                return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }

            return Mono.just("unknown");
        };
    }

    /**
     * Alternative key resolver for authenticated users (by user ID).
     * Can be used for per-user rate limiting when needed.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Extract user ID from JWT token if available
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }

            // Fall back to IP-based rate limiting
            return ipKeyResolver().resolve(exchange);
        };
    }
}
