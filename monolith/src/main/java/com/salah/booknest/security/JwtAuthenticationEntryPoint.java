package com.salah.booknest.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Runs when a request needs authentication but has none (token missing, invalid or expired).
 * Returns 401 instead of Spring Security's default 403.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Same shape as ApiError (fieldErrors is omitted because it is null)
        String path = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("""
                {
                    "timestamp": "%s",
                    "status": 401,
                    "error": "UNAUTHORIZED",
                    "message": "Authentication token is missing or invalid",
                    "path": "%s"
                }
                """.formatted(LocalDateTime.now(), path));
    }
}
