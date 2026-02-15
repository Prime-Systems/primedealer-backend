package com.prime.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception for validation errors with field-level details.
 */
@Getter
public class ValidationException extends BaseException {

    public static final String VALIDATION_FAILED = "VALIDATION_001";
    public static final String DUPLICATE_RESOURCE = "VALIDATION_002";
    public static final String INVALID_FORMAT = "VALIDATION_003";
    public static final String CONSTRAINT_VIOLATION = "VALIDATION_004";

    private final Map<String, String> fieldErrors;

    public ValidationException(String message, String errorCode, Map<String, String> fieldErrors) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors != null ? fieldErrors : new HashMap<>();
    }

    public ValidationException(String message, String errorCode) {
        this(message, errorCode, new HashMap<>());
    }

    public static ValidationException duplicateEmail(String email) {
        Map<String, String> errors = Map.of("email", "Email already registered: " + email);
        return new ValidationException("Email already exists", DUPLICATE_RESOURCE, errors);
    }

    public static ValidationException duplicateUsername(String username) {
        Map<String, String> errors = Map.of("username", "Username already taken: " + username);
        return new ValidationException("Username already exists", DUPLICATE_RESOURCE, errors);
    }

    public static  ValidationException duplicateGhanaCardNumber(String ghanaCardNumber) {
        Map<String, String> errors = Map.of("ghanaCardNumber", "Ghana Card Number already registered: " + ghanaCardNumber);
        return new ValidationException("Ghana Card Number already exists", DUPLICATE_RESOURCE, errors);
    }

    public static ValidationException invalidPassword() {
        Map<String, String> errors = Map.of("password", 
                "Password must be at least 12 characters with uppercase, lowercase, number, and special character");
        return new ValidationException("Invalid password format", INVALID_FORMAT, errors);
    }

    public static ValidationException weakPassword(String reason) {
        Map<String, String> errors = Map.of("password", reason);
        return new ValidationException("Weak password", INVALID_FORMAT, errors);
    }

    public static ValidationException withErrors(Map<String, String> fieldErrors) {
        return new ValidationException(
                String.format("Validation failed with %d error(s)", fieldErrors.size()),
                VALIDATION_FAILED,
                fieldErrors
        );
    }
}
