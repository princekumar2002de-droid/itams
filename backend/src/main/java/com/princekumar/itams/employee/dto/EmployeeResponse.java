package com.princekumar.itams.employee.dto;

import com.princekumar.itams.employee.EmploymentStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EmployeeResponse(
    Long id,
    Long personId,
    String firstName,
    String lastName,
    String email,
    String employeeNumber,
    Long departmentId,
    String departmentCode,
    String jobTitle,
    LocalDate hireDate,
    LocalDate endDate,
    EmploymentStatus employmentStatus,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
