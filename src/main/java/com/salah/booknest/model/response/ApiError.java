package com.salah.booknest.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** The single error format of every failing request. {@code fieldErrors} appears only for validation failures. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> fieldErrors) {
}