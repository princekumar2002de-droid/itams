package com.princekumar.itams.department.dto;

import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PATCH /api/v1/departments/{id}}.
 *
 * <p>All fields are optional; a {@code null} means "leave unchanged".
 * {@code code} is intentionally NOT updatable — changing a department's
 * short code would cascade through every printed label, badge and
 * reference. If a code really must change, that's a delete + create.</p>
 */
public record DepartmentUpdateRequest(

    @Size(max = 160)
    String name,

    Long parentDepartmentId,

    Long managerPersonId
) {}
