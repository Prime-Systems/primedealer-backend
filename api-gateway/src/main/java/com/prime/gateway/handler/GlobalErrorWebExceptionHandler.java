package com.prime.gateway.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global error handler for the API Gateway.
 * Provides consistent error responses for all gateway errors.
 */
@Slf4j
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                          WebProperties webProperties,
                                          ApplicationContext applicationContext,
                                          ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageReaders(serverCodecConfigurer.getReaders());
        super.setMessageWriters(serverCodecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        HttpStatus status = determineHttpStatus(error);

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", request.path());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", getErrorMessage(error, status));

        log.error("Gateway error: path={}, status={}, error={}", 
                request.path(), status, error.getMessage(), error);

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorResponse));
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
        return switch (status) {
            case SERVICE_UNAVAILABLE -> "Service temporarily unavailable. Please try again later.";
            case GATEWAY_TIMEOUT -> "Request timed out. Please try again.";
            case BAD_GATEWAY -> "Unable to connect to downstream service.";
            default -> "An unexpected error occurred.";
        };
    }
}
