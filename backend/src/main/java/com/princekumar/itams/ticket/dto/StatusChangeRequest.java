package com.princekumar.itams.ticket.dto;

import com.princekumar.itams.ticket.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(
    @NotNull TicketStatus to
) {}
