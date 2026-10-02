package com.princekumar.itams.ticket;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A comment on a ticket. Deliberately does not extend BaseEntity —
 * the schema has {@code created_at} but no {@code updated_at}.
 */
@Entity
@Table(name = "ticket_comment")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @Column(name = "author_user_id", nullable = false, updatable = false)
    private Long authorUserId;

    @Column(columnDefinition = "text", nullable = false, updatable = false)
    private String body;

    @Column(name = "is_internal", nullable = false, updatable = false)
    private boolean internal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected TicketComment() { /* JPA */ }

    public TicketComment(Ticket ticket, Long authorUserId, String body, boolean internal) {
        this.ticket = ticket;
        this.authorUserId = authorUserId;
        this.body = body;
        this.internal = internal;
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public Long getAuthorUserId() { return authorUserId; }
    public String getBody() { return body; }
    public boolean isInternal() { return internal; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TicketComment c)) return false;
        return id != null && id.equals(c.id);
    }
    @Override public int hashCode() { return Objects.hashCode(getClass()); }
}
