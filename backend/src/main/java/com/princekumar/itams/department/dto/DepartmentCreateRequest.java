package com.princekumar.itams.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/v1/departments}.
 *
 * @param code                 unique short code, e.g. "IT", "FIN", "MKT-EU"
 * @param name                 display name
 * @param parentDepartmentId   optional parent id
 * @param managerPersonId      optional manager person id
 */
public record DepartmentCreateRequest(

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^[A-Z0-9][A-Z0-9_-]*$",
             message = "code must be uppercase alphanumeric with '-' or '_' and start with a letter/digit")
    String code,

    @NotBlank
    @Size(max = 160)
    String name,

    Long parentDepartmentId,

    Long managerPersonId
) {}
