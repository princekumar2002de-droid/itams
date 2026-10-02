package com.princekumar.itams.assignment.dto;

import com.princekumar.itams.assignment.AssetCondition;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AssignAssetRequest(
    @NotNull Long assetId,
    @NotNull Long assigneePersonId,
    LocalDate expectedReturnOn,
    @NotNull AssetCondition outCondition,
    String notes
) {}
