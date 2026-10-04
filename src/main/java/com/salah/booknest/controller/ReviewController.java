package com.salah.booknest.controller;

import com.salah.booknest.model.request.ReviewRequest;
import com.salah.booknest.model.response.ReviewResponse;
import com.salah.booknest.model.response.ReviewSummary;
import com.salah.booknest.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/books/{bookId}/reviews")
    public ReviewSummary getBookReviews(@PathVariable Long bookId, Authentication authentication) {
        return reviewService.getBookReviews(bookId, authentication);
    }

    @PostMapping("/books/{bookId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(@PathVariable Long bookId,
                                                       @Valid @RequestBody ReviewRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(bookId, request, authentication));
    }

    @PutMapping("/reviews/{reviewId}")
    public ReviewResponse updateReview(@PathVariable Long reviewId,
                                       @RequestBody ReviewRequest request,
                                       Authentication authentication) {
        return reviewService.update(reviewId, request, authentication);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId, Authentication authentication) {
        reviewService.delete(reviewId, authentication);
        return ResponseEntity.noContent().build();
    }
}
