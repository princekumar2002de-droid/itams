package com.princekumar.itams.employee;

/**
 * Employment status. Stored as TEXT in the DB (with a CHECK constraint),
 * mapped to this enum in Java via {@code @Enumerated(EnumType.STRING)}.
 */
public enum EmploymentStatus {
    ACTIVE,
    ON_LEAVE,
    LEFT
}
