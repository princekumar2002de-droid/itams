package com.princekumar.itams.ticket.dto;

import java.time.OffsetDateTime;

public record TicketCommentResponse(
    Long id,
    Long authorUserId,
    String authorName,
    String body,
    boolean internal,
    OffsetDateTime createdAt
) {}
