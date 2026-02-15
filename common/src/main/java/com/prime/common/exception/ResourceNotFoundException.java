package com.prime.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for resource not found errors.
 */
public class ResourceNotFoundException extends BaseException {

    public static final String NOT_FOUND = "RESOURCE_001";

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found: %s", resourceType, identifier), 
              NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static ResourceNotFoundException user(String identifier) {
        return new ResourceNotFoundException("User", identifier);
    }

    public static ResourceNotFoundException session(String identifier) {
        return new ResourceNotFoundException("Session", identifier);
    }

    public static ResourceNotFoundException passkey(String identifier) {
        return new ResourceNotFoundException("Passkey", identifier);
    }

    public static ResourceNotFoundException token(String identifier) {
        return new ResourceNotFoundException("Token", identifier);
    }
}
