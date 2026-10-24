package com.salah.booknest.model.response;

import com.salah.booknest.model.WaitlistEntry;
import com.salah.booknest.model.WaitlistStatus;

import java.time.LocalDateTime;

public record WaitlistResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long userId,
        String username,
        WaitlistStatus status,
        int position,
        LocalDateTime joinedAt) {

    public static WaitlistResponse from(WaitlistEntry entry, int position) {
        return new WaitlistResponse(
                entry.getId(),
                entry.getBook().getId(),
                entry.getBook().getTitle(),
                entry.getUser().getId(),
                entry.getUser().getUsername(),
                entry.getStatus(),
                position,
                entry.getJoinedAt());
    }
}
