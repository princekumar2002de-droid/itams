package com.princekumar.itams.assignment.dto;

import com.princekumar.itams.assignment.AssetCondition;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AssetAssignmentResponse(
    Long id,
    Long assetId,
    String assetTag,
    Long assigneePersonId,
    String assigneeName,
    Long assignedByUserId,
    OffsetDateTime assignedAt,
    LocalDate expectedReturnOn,
    OffsetDateTime actualReturnAt,
    Long returnedByUserId,
    AssetCondition outCondition,
    AssetCondition inCondition,
    String notes,
    boolean open
) {}
