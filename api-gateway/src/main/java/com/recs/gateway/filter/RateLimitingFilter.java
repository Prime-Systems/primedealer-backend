package com.prime.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Rate limiting filter using Redis.
 * Implements sliding window rate limiting per client IP.
 */
@Slf4j
@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitingFilter(ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientId = getClientIdentifier(exchange);
            String key = "rate_limit:" + config.getKeyPrefix() + ":" + clientId;

            return redisTemplate.opsForValue().increment(key)
                    .flatMap(count -> {
                        if (count == 1) {
                            // Set TTL on first request
                            return redisTemplate.expire(key, Duration.ofSeconds(config.getWindowSeconds()))
                                    .thenReturn(count);
                        }
                        return Mono.just(count);
                    })
                    .flatMap(count -> {
                        // Add rate limit headers
                        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", 
                                String.valueOf(config.getMaxRequests()));
                        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", 
                                String.valueOf(Math.max(0, config.getMaxRequests() - count)));

                        if (count > config.getMaxRequests()) {
                            log.warn("Rate limit exceeded for client: {}", clientId);
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            exchange.getResponse().getHeaders().add("Retry-After", 
                                    String.valueOf(config.getWindowSeconds()));
                            return exchange.getResponse().setComplete();
                        }

                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> {
                        // On Redis error, allow request (fail open)
                        log.error("Rate limiting error: {}", e.getMessage());
                        return chain.filter(exchange);
                    });
        };
    }

    private String getClientIdentifier(org.springframework.web.server.ServerWebExchange exchange) {
        // Try X-Forwarded-For first (for clients behind proxy)
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }

        // Fall back to remote address
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    public static class Config {
        private int maxRequests = 100;
        private int windowSeconds = 60;
        private String keyPrefix = "default";

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }
}
