package com.salah.booknest.model.response;

import java.util.List;

public record ReviewSummary(
        List<ReviewResponse> reviews,
        Double averageRating,
        int reviewCount,
        boolean canReview,
        Long myReviewId) {
}
