package com.salah.booknest.lending.model.response;

import com.salah.booknest.lending.model.WaitlistEntry;
import com.salah.booknest.lending.model.WaitlistStatus;

import java.time.LocalDateTime;

public record WaitlistResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long userId,
        String username,
        WaitlistStatus status,
        int position,
        LocalDateTime holdExpiresAt,
        LocalDateTime joinedAt) {

    public static WaitlistResponse from(WaitlistEntry entry, int position) {
        return new WaitlistResponse(
                entry.getId(),
                entry.getBookId(),
                entry.getBookTitle(),
                entry.getUserId(),
                entry.getUsername(),
                entry.getStatus(),
                position,
                entry.getHoldExpiresAt(),
                entry.getJoinedAt());
    }
}
