package com.princekumar.itams.common.exception;

/**
 * Thrown when a request is well-formed but violates a business rule
 * (e.g. attempting to assign an already-assigned asset, exceeding
 * license seats, illegal state-machine transition).
 *
 * <p>Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 * The {@code code} is a stable machine-readable identifier — clients
 * localise or branch on the code, not on the free-text message.</p>
 */
public class BusinessRuleViolationException extends RuntimeException {

    private final String code;

    public BusinessRuleViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
