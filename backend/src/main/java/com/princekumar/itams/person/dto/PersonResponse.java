package com.princekumar.itams.person.dto;

import java.time.OffsetDateTime;

public record PersonResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    String phone,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
