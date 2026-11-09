package com.salah.booknest.notification.event;

import java.time.LocalDateTime;

/**
 * Read from the loan-events and waitlist-events topics (sent by lending-service).
 *
 * @param audience MEMBER: live notification and email to the member, LIBRARIANS: live notification to every librarian
 */
public record LendingEvent(
        String eventId,
        String type,
        String audience,
        Long userId,
        String username,
        Long loanId,
        String status,
        String message,
        LocalDateTime occurredAt) {

    public boolean forLibrarians() {
        return "LIBRARIANS".equals(audience);
    }
}
