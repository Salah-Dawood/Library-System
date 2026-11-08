package com.salah.booknest.model.response;

import com.salah.booknest.model.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long bookId,
        String username,
        Integer rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean mine) {

    public static ReviewResponse from(Review review, Long currentUserId) {
        return new ReviewResponse(
                review.getId(),
                review.getBook().getId(),
                review.getUser().getUsername(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getUser().getId().equals(currentUserId));
    }
}
