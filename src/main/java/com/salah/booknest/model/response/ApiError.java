package com.salah.booknest.model.response;

import java.time.LocalDateTime;

/** The single error format returned by every failing request. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {
}