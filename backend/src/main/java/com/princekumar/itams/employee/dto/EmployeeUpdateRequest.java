package com.princekumar.itams.employee.dto;

import com.princekumar.itams.employee.EmploymentStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** All fields nullable; {@code null} means "leave unchanged". */
public record EmployeeUpdateRequest(
    Long departmentId,
    @Size(max = 120) String jobTitle,
    LocalDate endDate,
    EmploymentStatus employmentStatus
) {}
