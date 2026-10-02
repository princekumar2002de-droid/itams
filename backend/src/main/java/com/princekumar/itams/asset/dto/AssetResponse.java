package com.princekumar.itams.asset.dto;

import com.princekumar.itams.asset.AssetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AssetResponse(
    Long id,
    String assetTag,
    Long modelId,
    String modelManufacturer,
    String modelName,
    String categoryCode,
    String serialNumber,
    AssetStatus status,
    LocalDate purchaseDate,
    BigDecimal purchasePrice,
    LocalDate warrantyEndsOn,
    String notes,
    OffsetDateTime retiredAt,
    String retiredReason,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
