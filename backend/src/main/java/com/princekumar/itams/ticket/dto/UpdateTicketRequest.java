package com.princekumar.itams.ticket.dto;

import com.princekumar.itams.ticket.TicketPriority;

/** Nullable fields — null means "leave unchanged". */
public record UpdateTicketRequest(
    TicketPriority priority,
    Long assignedToUserId
) {}
