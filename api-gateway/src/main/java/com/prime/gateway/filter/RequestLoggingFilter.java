package com.prime.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Request logging and tracing filter.
 * Adds correlation IDs and logs request/response details.
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_START_TIME = "requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate or use existing request ID
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        // Add request ID to response headers
        String finalRequestId = requestId;
        exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

        // Store start time for duration calculation
        exchange.getAttributes().put(REQUEST_START_TIME, Instant.now());

        // Add request ID to the request
        ServerHttpRequest enrichedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();

        // Log incoming request
        log.info("Incoming request: method={}, path={}, requestId={}",
                request.getMethod(),
                request.getPath(),
                requestId);

        return chain.filter(exchange.mutate().request(enrichedRequest).build())
                .doFinally(signalType -> {
                    Instant startTime = exchange.getAttribute(REQUEST_START_TIME);
                    long duration = startTime != null ? 
                            Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;

                    log.info("Completed request: method={}, path={}, status={}, duration={}ms, requestId={}",
                            request.getMethod(),
                            request.getPath(),
                            exchange.getResponse().getStatusCode(),
                            duration,
                            finalRequestId);
                });
    }

    @Override
    public int getOrder() {
        return -200; // Run before authentication filter
    }
}
