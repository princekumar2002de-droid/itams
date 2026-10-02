package com.princekumar.itams.maintenance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record MaintenanceResponse(
    Long id,
    Long assetId,
    String assetTag,
    LocalDate performedOn,
    Long performedByUserId,
    String providerName,
    String description,
    BigDecimal cost,
    LocalDate nextScheduledOn,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
