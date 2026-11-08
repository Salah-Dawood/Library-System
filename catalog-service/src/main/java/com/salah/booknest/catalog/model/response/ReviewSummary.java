package com.salah.booknest.catalog.model.response;

import java.util.List;

public record ReviewSummary(
        List<ReviewResponse> reviews,
        Double averageRating,
        int reviewCount,
        boolean canReview,
        Long myReviewId) {
}
