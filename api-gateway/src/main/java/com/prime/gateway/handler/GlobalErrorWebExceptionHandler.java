package com.prime.gateway.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global error handler for the API Gateway.
 * Provides consistent error responses for all gateway errors.
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    @Override
    @SuppressWarnings("NullMarked")
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = determineHttpStatus(ex);

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", exchange.getRequest().getPath().value());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", getErrorMessage(ex, status));

        log.error("Gateway error: path={}, status={}, error={}", 
                exchange.getRequest().getPath().value(), status, ex.getMessage(), ex);

        // Set response status and content type
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        // Write error response
        String jsonResponse;
        try {
            // Simple JSON serialization since we don't have ObjectMapper dependency
            jsonResponse = String.format(
                "{\"success\":%s,\"timestamp\":\"%s\",\"path\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                false,
                errorResponse.get("timestamp"),
                errorResponse.get("path"),
                status.value(),
                status.getReasonPhrase(),
                errorResponse.get("message")
            );
        } catch (Exception e) {
            jsonResponse = "{\"success\":false,\"message\":\"An error occurred\"}";
        }

        var buffer = exchange.getResponse().bufferFactory().wrap(jsonResponse.getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private HttpStatus determineHttpStatus(Throwable error) {
        String errorName = error.getClass().getSimpleName();

        if (errorName.contains("NotFound") || errorName.contains("ServiceUnavailable")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (errorName.contains("Timeout") || errorName.contains("CircuitBreaker")) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (errorName.contains("Connection")) {
            return HttpStatus.BAD_GATEWAY;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String getErrorMessage(Throwable error, HttpStatus status) {
        // Check for specific error types for more detailed messages
        String errorMessage = error.getMessage();
        if (errorMessage != null && !errorMessage.isEmpty()) {
            // Return more specific error messages based on status and exception details
            return switch (status) {
                case SERVICE_UNAVAILABLE -> "Service temporarily unavailable: " + errorMessage;
                case GATEWAY_TIMEOUT -> "Request timed out: " + errorMessage;
                case BAD_GATEWAY -> "Unable to connect to downstream service: " + errorMessage;
                default -> "An unexpected error occurred: " + errorMessage;
            };
        }

        // Fallback to generic messages if no specific error message available
        return switch (status) {
            case SERVICE_UNAVAILABLE -> "Service temporarily unavailable. Please try again later.";
            case GATEWAY_TIMEOUT -> "Request timed out. Please try again.";
            case BAD_GATEWAY -> "Unable to connect to downstream service.";
            default -> "An unexpected error occurred.";
        };
    }
}
