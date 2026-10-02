package com.princekumar.itams.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload for creating an employee.
 * The referenced person and department must already exist.
 */
public record EmployeeCreateRequest(
    @NotNull  Long personId,
    @NotBlank @Size(max = 30)
    @Pattern(regexp = "^[A-Z0-9-]+$",
             message = "employeeNumber must be uppercase alphanumeric or dashes")
    String employeeNumber,
    @NotNull  Long departmentId,
    @Size(max = 120) String jobTitle,
    @NotNull  LocalDate hireDate
) {}
