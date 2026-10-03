package com.salah.booknest.model.response;

import com.salah.booknest.model.LoanStatus;

import java.time.LocalDateTime;

/** Payload pushed to clients over Server-Sent Events. {@code type} is also the SSE event name. */
public record NotificationEvent(
        String type,
        String message,
        Long loanId,
        LoanStatus status,
        LocalDateTime timestamp) {
}