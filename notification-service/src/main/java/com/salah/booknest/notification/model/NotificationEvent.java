package com.salah.booknest.notification.model;

import java.time.LocalDateTime;

/** What the browser receives on the live notification stream. */
public record NotificationEvent(
        String type,
        String message,
        Long loanId,
        String status,
        LocalDateTime timestamp) {
}
