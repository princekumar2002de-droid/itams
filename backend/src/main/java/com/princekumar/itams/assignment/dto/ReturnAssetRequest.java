package com.princekumar.itams.assignment.dto;

import com.princekumar.itams.assignment.AssetCondition;
import jakarta.validation.constraints.NotNull;

public record ReturnAssetRequest(
    @NotNull AssetCondition inCondition,
    boolean sendForMaintenance,
    String notes
) {}
