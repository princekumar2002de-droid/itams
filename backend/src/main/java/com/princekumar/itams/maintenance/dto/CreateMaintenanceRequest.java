package com.princekumar.itams.maintenance.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * At least one of performedByUserId / providerName must be non-null
 * (DB check constraint). Service also validates.
 */
public record CreateMaintenanceRequest(
    @NotNull Long assetId,
    @NotNull @PastOrPresent LocalDate performedOn,
    Long performedByUserId,
    @Size(max = 160) String providerName,
    @NotBlank String description,
    @DecimalMin("0.00") BigDecimal cost,
    LocalDate nextScheduledOn
) {}
