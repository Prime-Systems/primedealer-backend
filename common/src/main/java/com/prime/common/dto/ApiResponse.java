package com.prime.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized API response wrapper following API-first design principles.
 * All API responses follow this consistent structure for predictable client handling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorDetails error;
    private String requestId;
    
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Create a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .requestId(requestId)
                .build();
    }

    /**
     * Create a successful response with data (without requestId).
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Create a simple error response (without requestId).
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetails.builder()
                        .code("ERROR")
                        .message(message)
                        .build())
                .build();
    }

    /**
     * Create an error response.
     */
    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .build())
                .requestId(requestId)
                .build();
    }

    /**
     * Create an error response with field-level details.
     */
    public static <T> ApiResponse<T> error(String code, String message, 
                                            Map<String, String> details, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(ErrorDetails.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .requestId(requestId)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private String code;
        private String message;
        private Map<String, String> details;
    }
}
