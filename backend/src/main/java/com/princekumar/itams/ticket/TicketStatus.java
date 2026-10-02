package com.princekumar.itams.ticket;

/**
 * Ticket lifecycle:
 *   OPEN ─pick up→ IN_PROGRESS ─need info→ WAITING ─reply→ IN_PROGRESS
 *                                            │
 *                                            └─resolve→ RESOLVED ─close→ CLOSED
 */
public enum TicketStatus {
    OPEN, IN_PROGRESS, WAITING, RESOLVED, CLOSED
}
