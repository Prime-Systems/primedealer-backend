package com.prime.common.exception;

import com.prime.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.prime.common.security.SecurityConstants.REQUEST_ID_HEADER;

/**
 * Global exception handler for consistent error responses across all services.
 * Follows API-first design principles with structured error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle all domain-specific exceptions.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(
            BaseException ex, HttpServletRequest request) {
        
        String requestId = getRequestId(request);
        
        log.warn("Domain exception occurred: code={}, message={}, requestId={}",
                ex.getErrorCode(), ex.getMessage(), requestId);

        ApiResponse<Void> response;
        
        if (ex instanceof ValidationException validationEx) {
            response = ApiResponse.error(
                    ex.getErrorCode(),
                    ex.getMessage(),
                    validationEx.getFieldErrors(),
                    requestId
            );
        } else {
            response = ApiResponse.error(
                    ex.getErrorCode(),
                    ex.getMessage(),
                    requestId
            );
        }

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Handle Spring validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String requestId = getRequestId(request);
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: errors={}, requestId={}", errors, requestId);

        ApiResponse<Void> response = ApiResponse.error(
                ValidationException.VALIDATION_FAILED,
                String.format("Validation failed with %d error(s)", errors.size()),
                errors,
                requestId
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        String requestId = getRequestId(request);
        
        log.warn("Access denied: message={}, requestId={}", ex.getMessage(), requestId);

        ApiResponse<Void> response = ApiResponse.error(
                AuthenticationException.INSUFFICIENT_PERMISSIONS,
                "Access denied",
                requestId
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String requestId = getRequestId(request);
        
        log.error("Unexpected error occurred: message={}, requestId={}", 
                ex.getMessage(), requestId, ex);

        // Don't expose internal error details to clients
        ApiResponse<Void> response = ApiResponse.error(
                "INTERNAL_ERROR",
                "An internal error occurred. Please contact support with request ID: " + requestId,
                requestId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
