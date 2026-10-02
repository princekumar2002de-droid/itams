package com.princekumar.itams.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.princekumar.itams.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Emits our standard {@link ErrorResponse} shape for 401 (unauthenticated)
 * — replaces Spring Security's default HTML page.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper json;

    public RestAuthenticationEntryPoint(ObjectMapper json) { this.json = json; }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorResponse body = new ErrorResponse(
            "auth.unauthenticated",
            "Authentication required.",
            HttpStatus.UNAUTHORIZED.value(),
            request.getRequestURI(),
            OffsetDateTime.now(),
            null
        );
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(json.writeValueAsString(body));
    }
}
