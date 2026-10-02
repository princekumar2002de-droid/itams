package com.princekumar.itams.ticket.dto;

import com.princekumar.itams.ticket.TicketPriority;
import com.princekumar.itams.ticket.TicketStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record TicketResponse(
    Long id,
    String ticketNumber,
    String subject,
    String description,
    TicketPriority priority,
    TicketStatus status,
    Long reporterPersonId,
    String reporterName,
    Long relatedAssetId,
    String relatedAssetTag,
    Long assignedToUserId,
    OffsetDateTime resolvedAt,
    OffsetDateTime closedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    /** Only present on the single-ticket endpoint — null on list responses. */
    List<TicketCommentResponse> comments
) {}
