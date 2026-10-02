package com.princekumar.itams.license.dto;

/** Exactly one of personId / assetId must be non-null. Enforced in the service. */
public record AssignLicenseRequest(
    Long personId,
    Long assetId,
    String notes
) {}
