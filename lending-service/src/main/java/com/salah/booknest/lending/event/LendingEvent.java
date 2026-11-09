package com.salah.booknest.lending.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sent to the loan-events and waitlist-events topics. notification-service turns it into a live
 * notification, plus an email when it is meant for the member.
 *
 * @param audience MEMBER for the member's own loan or hold, LIBRARIANS for every librarian
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

    public static LendingEvent forMember(String type, Long userId, String username, Long loanId, String status, String message) {
        return new LendingEvent(UUID.randomUUID().toString(), type, "MEMBER", userId, username, loanId, status, message, LocalDateTime.now());
    }

    public static LendingEvent forLibrarians(String type, Long userId, String username, Long loanId, String status, String message) {
        return new LendingEvent(UUID.randomUUID().toString(), type, "LIBRARIANS", userId, username, loanId, status, message, LocalDateTime.now());
    }
}
