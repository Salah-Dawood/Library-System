package com.salah.booknest.user.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sent to the user-events topic when someone registers, so notification-service knows where to send emails.
 */
public record UserRegisteredEvent(String eventId, Long userId, String username, String email, LocalDateTime occurredAt) {

    public static UserRegisteredEvent of(Long userId, String username, String email) {
        return new UserRegisteredEvent(UUID.randomUUID().toString(), userId, username, email, LocalDateTime.now());
    }
}
