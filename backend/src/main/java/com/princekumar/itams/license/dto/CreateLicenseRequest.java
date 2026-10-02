package com.princekumar.itams.license.dto;

import com.princekumar.itams.license.LicenseType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Product info is nested — this endpoint upserts the product (matched by
 * vendor + name + version) so callers don't need a separate CRUD.
 */
public record CreateLicenseRequest(
    @NotBlank @Size(max = 120) String vendor,
    @NotBlank @Size(max = 160) String productName,
    @Size(max = 60)            String productVersion,

    @NotBlank @Size(max = 120) String licenseReference,
    @NotNull                   LicenseType licenseType,
    @Positive                  int seatsTotal,
    @NotNull @PastOrPresent    LocalDate purchaseDate,
    @FutureOrPresent           LocalDate expiresOn,
    @NotNull @DecimalMin("0.00") BigDecimal cost,
    @Size(max = 120)           String procurementRef
) {}
