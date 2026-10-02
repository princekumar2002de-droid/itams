package com.princekumar.itams.common.exception;

/**
 * Thrown when a requested resource does not exist. Mapped to HTTP 404
 * by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super("%s with id %s not found".formatted(resource, id));
    }
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
