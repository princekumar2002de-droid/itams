package com.princekumar.itams.ticket.dto;

import com.princekumar.itams.ticket.TicketPriority;
import jakarta.validation.constraints.*;

public record CreateTicketRequest(
    @NotBlank @Size(max = 200) String subject,
    @NotBlank                  String description,
    @NotNull                   TicketPriority priority,
    @NotNull                   Long reporterPersonId,
    Long relatedAssetId
) {}
