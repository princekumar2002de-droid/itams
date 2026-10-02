package com.princekumar.itams.asset.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetCreateRequest(
    @NotBlank @Size(max = 30)
    @Pattern(regexp = "^[A-Z0-9-]+$",
             message = "assetTag must be uppercase alphanumeric or dashes")
    String assetTag,
    @NotNull  Long modelId,
    @NotBlank @Size(max = 80)  String serialNumber,
    @NotNull  @PastOrPresent   LocalDate purchaseDate,
    @NotNull  @DecimalMin("0.00") BigDecimal purchasePrice,
    LocalDate warrantyEndsOn,
    String notes
) {}
