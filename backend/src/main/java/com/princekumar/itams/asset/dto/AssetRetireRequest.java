package com.princekumar.itams.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssetRetireRequest(
    @NotBlank @Size(max = 255) String reason
) {}
