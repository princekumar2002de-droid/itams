package com.princekumar.itams.license.dto;

import java.time.OffsetDateTime;

public record LicenseAssignmentResponse(
    Long id,
    Long licenseId,
    Long personId,
    String personName,
    Long assetId,
    String assetTag,
    OffsetDateTime assignedAt,
    OffsetDateTime releasedAt,
    String notes,
    boolean open
) {}
