package com.princekumar.itams.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * JPA entity backing the pre-existing {@code audit_log} table (created in
 * the V1 Flyway baseline). Does not extend {@link com.princekumar.itams.common.entity.BaseEntity}
 * because the DDL deliberately uses {@code occurred_at} (not {@code created_at})
 * and does not carry {@code updated_at} — audit rows are append-only.
 *
 * <p>The DB-level {@code CHECK (action IN (...))} constraint restricts the
 * allowed action values; see {@link AuditWrite#action()} for the Java-side
 * values used today.</p>
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false, columnDefinition = "text")
    private String action;

    /**
     * Freeform JSON string with change details, stored in a {@code jsonb} column.
     * {@code @JdbcTypeCode(SqlTypes.JSON)} makes Hibernate 6 bind it as JSON (no extra
     * dependency). Without it the value was bound as VARCHAR and Postgres rejected
     * every insert ("column changes is of type jsonb but expression is of type
     * character varying") and the audit row was not stored.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String changes;

    @Column(name = "request_id", length = 60)
    private String requestId;

    protected AuditLog() { /* JPA */ }

    public AuditLog(OffsetDateTime occurredAt, Long actorUserId,
                    String entityType, Long entityId,
                    String action, String changes, String requestId) {
        this.occurredAt  = occurredAt;
        this.actorUserId = actorUserId;
        this.entityType  = entityType;
        this.entityId    = entityId;
        this.action      = action;
        this.changes     = changes;
        this.requestId   = requestId;
    }

    public Long getId()                  { return id; }
    public OffsetDateTime getOccurredAt(){ return occurredAt; }
    public Long getActorUserId()         { return actorUserId; }
    public String getEntityType()        { return entityType; }
    public Long getEntityId()            { return entityId; }
    public String getAction()            { return action; }
    public String getChanges()           { return changes; }
    public String getRequestId()         { return requestId; }
}
