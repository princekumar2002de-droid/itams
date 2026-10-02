package com.princekumar.itams.license.dto;

import com.princekumar.itams.license.LicenseType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record LicenseResponse(
    Long id,
    Long productId,
    String productVendor,
    String productName,
    String productVersion,
    String licenseReference,
    LicenseType licenseType,
    int seatsTotal,
    int seatsUsed,
    int seatsAvailable,
    LocalDate purchaseDate,
    LocalDate expiresOn,
    BigDecimal cost,
    String procurementRef,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
