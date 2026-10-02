package com.princekumar.itams.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.princekumar.itams.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Emits our standard {@link ErrorResponse} shape for 403 (forbidden) —
 * authenticated but not authorised for this endpoint.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper json;

    public RestAccessDeniedHandler(ObjectMapper json) { this.json = json; }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ErrorResponse body = new ErrorResponse(
            "auth.forbidden",
            "You do not have permission to perform this action.",
            HttpStatus.FORBIDDEN.value(),
            request.getRequestURI(),
            OffsetDateTime.now(),
            null
        );
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(json.writeValueAsString(body));
    }
}
