package com.princekumar.itams.asset.dto;

import java.time.LocalDate;

/** All fields nullable. assetTag / modelId / serialNumber are IMMUTABLE. */
public record AssetUpdateRequest(
    LocalDate warrantyEndsOn,
    String notes
) {}
