package com.princekumar.itams.common.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Stable error payload for every non-2xx response.
 *
 * @param code       machine-readable identifier ("resource.not_found", "validation.failed", ...)
 * @param message    human-readable summary
 * @param status     HTTP status code
 * @param path       request path
 * @param timestamp  when the error was produced (server clock, UTC)
 * @param details    optional field-level errors (populated for validation failures)
 */
public record ErrorResponse(
    String code,
    String message,
    int status,
    String path,
    OffsetDateTime timestamp,
    List<FieldError> details
) {
    public record FieldError(String field, String message) {}
}
