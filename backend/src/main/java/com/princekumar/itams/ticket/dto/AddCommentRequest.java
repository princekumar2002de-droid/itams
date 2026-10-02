package com.princekumar.itams.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(
    @NotBlank String body,
    boolean internal
) {}
