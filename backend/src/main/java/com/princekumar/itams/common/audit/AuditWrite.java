package com.princekumar.itams.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose successful return should produce one row
 * in {@code audit_log}. The {@link AuditAspect} picks these up at runtime.
 *
 * <p>Only applies to Spring beans — a direct {@code new TicketService(...)}
 * call in a unit test will NOT trigger the aspect (that is intentional:
 * aspect behaviour is verified by {@code AuditAspectIT}, not by every
 * individual service test).</p>
 *
 * <p>{@code action} must be one of the values permitted by the DB-level
 * CHECK constraint on {@code audit_log.action}:
 * {@code CREATE, UPDATE, DELETE, ASSIGN, RETURN, STATUS_CHANGE,
 *        LOGIN, LOGOUT, TOKEN_REFRESH, TOKEN_REVOKE}.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditWrite {

    /** Entity type as it appears in {@code audit_log.entity_type}, e.g. "Ticket", "Asset". */
    String entity();

    /** One of the DB-constrained action values. */
    String action();
}
