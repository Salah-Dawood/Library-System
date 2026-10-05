package com.salah.booknest.exception;

import com.salah.booknest.model.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InformationNotFoundException.class)
    public ResponseEntity<ApiError> notFound(InformationNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(InformationExistException.class)
    public ResponseEntity<ApiError> alreadyExists(InformationExistException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ApiError> invalidState(InvalidStateException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "INVALID_STATE", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> invalidRequest(InvalidRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to perform this action", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> authenticationFailed(AuthenticationException ex, HttpServletRequest req) {
        log.warn("Failed authentication attempt on {}", req.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid username or password", req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParameter(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Invalid value for parameter '" + ex.getName() + "'", req);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> accountDeactivated(DisabledException ex, HttpServletRequest req) {
        log.warn("Login attempt on a deactivated account");
        return build(HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED", "This account has been deactivated", req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> dataConflict(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation on {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "The request conflicts with existing data, for example the item is still in use", req);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void clientDisconnected() {
        log.debug("Client disconnected from a streaming response");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        // Spring's own web exceptions (404 for unknown URLs, 405, ...) already know their status.
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return build(status, status.name(), status.getReasonPhrase(), req);
        }
        log.error("Unhandled exception on {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message, HttpServletRequest req) {
        ApiError body = new ApiError(LocalDateTime.now(), status.value(), error, message, req.getRequestURI(), null);
        return ResponseEntity.status(status).body(body);
    }

    /** Failed @Valid checks, for JSON bodies and form data: one message per invalid field. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> invalidFields(BindException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.merge(error.getField(), String.valueOf(error.getDefaultMessage()), (a, b) -> a + "; " + b));
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                fieldErrors.put(error.getObjectName(), String.valueOf(error.getDefaultMessage())));

        ApiError body = new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED",
                "Validation failed for " + fieldErrors.size() + " field(s)", req.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }
}