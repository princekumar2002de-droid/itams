package com.princekumar.itams.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetModelCreateRequest(
    @NotNull  Long categoryId,
    @NotBlank @Size(max = 80)  String manufacturer,
    @NotBlank @Size(max = 160) String modelName,
    String specs   // JSON string, optional
) {}
