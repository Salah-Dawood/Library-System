package com.salah.booknest.notification.event;

import java.time.LocalDateTime;

/** Read from the user-events topic (sent by user-service). */
public record UserRegisteredEvent(String eventId, Long userId, String username, String email, LocalDateTime occurredAt) {
}
