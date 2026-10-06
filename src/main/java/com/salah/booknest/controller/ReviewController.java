package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Reviews", description = "1-10 ratings with an optional description, written by members who returned the book.")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "List a book's reviews", description = "All reviews plus the average rating. canReview tells the caller whether they may write one; myReviewId identifies their own review.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No book with this id")
    })
    @GetMapping("/books/{bookId}/reviews")
    public ReviewSummary getBookReviews(@Parameter(description = "Id of the book") @PathVariable Long bookId, Authentication authentication) {
        return reviewService.getBookReviews(bookId, authentication);
    }

    @Operation(summary = "Review a book", description = "Only members who returned the book can review it, once. Rating is 1 to 10.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review created", content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "404", description = "No book with this id"),
            @ApiResponse(responseCode = "409", description = "The book has not been returned, or you already reviewed it")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"rating\":8,\"comment\":\"Great world-building.\"}")))
    @PostMapping("/books/{bookId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(@Parameter(description = "Id of the book") @PathVariable Long bookId,
                                                       @Valid @RequestBody ReviewRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(bookId, request, authentication));
    }

    @Operation(summary = "Edit my review", description = "Only the author of the review can edit it.")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = "Not your review"),
            @ApiResponse(responseCode = "404", description = "No review with this id")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"rating\":9,\"comment\":\"Even better on a second read.\"}")))
    @PutMapping("/reviews/{reviewId}")
    public ReviewResponse updateReview(@Parameter(description = "Id of the review") @PathVariable Long reviewId,
                                       @Valid @RequestBody ReviewRequest request,
                                       Authentication authentication) {
        return reviewService.update(reviewId, request, authentication);
    }

    @Operation(summary = "Delete a review", description = "The author can delete their review. Librarians can delete any review.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Review deleted"),
            @ApiResponse(responseCode = "403", description = "Not your review"),
            @ApiResponse(responseCode = "404", description = "No review with this id")
    })
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@Parameter(description = "Id of the review") @PathVariable Long reviewId, Authentication authentication) {
        reviewService.delete(reviewId, authentication);
        return ResponseEntity.noContent().build();
    }
}
