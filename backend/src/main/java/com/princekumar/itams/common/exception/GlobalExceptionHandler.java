package com.princekumar.itams.common.exception;

import com.princekumar.itams.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Turns exceptions into stable {@link ErrorResponse} bodies.
 * Nothing here leaks a stack trace to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "resource.not_found", ex.getMessage(), req, null);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
        return build(HttpStatus.BAD_REQUEST, "validation.failed", "Request validation failed", req, details);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation on {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "data.integrity_violation",
            "The request conflicts with existing data (uniqueness, foreign key or check constraint).", req, null);
    }

    /**
     * Domain state-machine violations (e.g. Asset.markAssigned when the asset is
     * already assigned; Ticket.transitionTo with an illegal transition) bubble up
     * as {@link IllegalStateException}. Services normally wrap these into
     * {@link BusinessRuleViolationException}, but this handler is the safety net
     * so a missed wrap still returns 409 rather than 500.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        log.debug("Illegal state on {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "business.illegal_state", ex.getMessage(), req, null);
    }

    /**
     * Bad arguments the domain rejects (bounds, ranges) that weren't caught by
     * bean validation. These are client errors, so 400 rather than 500.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "request.illegal_argument", ex.getMessage(), req, null);
    }

    /** Malformed / unparseable JSON body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "request.malformed",
            "Request body could not be parsed as JSON.", req, null);
    }

    /** Wrong type on a path variable / query parameter — e.g. `/assets/abc` where a number is required. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = "Parameter '%s' has the wrong type (expected %s).".formatted(
            ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "correct type");
        return build(HttpStatus.BAD_REQUEST, "request.type_mismatch", msg, req, null);
    }

    // ── Authentication / authorization ────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        // Generic 401 — never leak whether the username existed.
        return build(HttpStatus.UNAUTHORIZED, "auth.bad_credentials", "Invalid username or password.", req, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "auth.account_disabled", "This account is disabled.", req, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "auth.unauthenticated", "Authentication failed.", req, null);
    }

    /**
     * @PreAuthorize denials are thrown INSIDE the DispatcherServlet, so they reach this
     * advice before Spring Security's ExceptionTranslationFilter (and its
     * RestAccessDeniedHandler) ever sees them. Without this handler the catch-all below
     * turned every "wrong role" request into a 500. Same body as RestAccessDeniedHandler.
     * (Spring Security 6.3 throws AuthorizationDeniedException, a subclass.)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "auth.forbidden",
            "You do not have permission to perform this action.", req, null);
    }

    // ── Fallback ──────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal.error",
            "An unexpected error occurred. See server logs.", req, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                 HttpServletRequest req, List<ErrorResponse.FieldError> details) {
        ErrorResponse body = new ErrorResponse(
            code, message, status.value(), req.getRequestURI(), OffsetDateTime.now(), details
        );
        return ResponseEntity.status(status).body(body);
    }
}
