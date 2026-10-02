package com.princekumar.itams.department.dto;

import java.time.OffsetDateTime;

public record DepartmentResponse(
    Long id,
    String code,
    String name,
    Long parentDepartmentId,
    Long managerPersonId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
