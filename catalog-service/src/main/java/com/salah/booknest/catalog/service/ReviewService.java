package com.salah.booknest.catalog.service;

import com.salah.booknest.catalog.client.LendingClient;
import com.salah.booknest.catalog.exception.InformationExistException;
import com.salah.booknest.catalog.exception.InformationNotFoundException;
import com.salah.booknest.catalog.exception.InvalidRequestException;
import com.salah.booknest.catalog.exception.InvalidStateException;
import com.salah.booknest.catalog.model.Book;
import com.salah.booknest.catalog.model.Review;
import com.salah.booknest.catalog.model.request.ReviewRequest;
import com.salah.booknest.catalog.model.response.ReviewResponse;
import com.salah.booknest.catalog.model.response.ReviewSummary;
import com.salah.booknest.catalog.repository.BookRepository;
import com.salah.booknest.catalog.repository.ReviewRepository;
import com.salah.booknest.catalog.security.Roles;
import com.salah.booknest.catalog.security.TokenUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 10;
    private static final int MAX_COMMENT_LENGTH = 1000;

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final LendingClient lendingClient;

    /**
     * Returns all reviews of a book with the average rating, plus whether the current user can still
     * write a review (they returned the book and have not reviewed it yet) and the id of their own review if any.
     */
    @Transactional(readOnly = true)
    public ReviewSummary getBookReviews(Long bookId, Authentication authentication) {
        getBook(bookId);
        TokenUser user = getUser(authentication);
        List<Review> reviews = reviewRepository.findAllByBookIdOrderByCreatedAtDesc(bookId);
        Long myReviewId = reviews.stream()
                .filter(review -> review.getUserId().equals(user.id()))
                .map(Review::getId).findFirst().orElse(null);
        Double average = reviews.isEmpty() ? null
                : Math.round(reviews.stream().mapToInt(Review::getRating).average().orElse(0) * 10) / 10.0;
        return new ReviewSummary(
                reviews.stream().map(review -> ReviewResponse.from(review, user.id())).toList(),
                average,
                reviews.size(),
                myReviewId == null && lendingClient.hasReturned(bookId),
                myReviewId);
    }

    /**
     * Adds a review. Only members who have returned the book may review it, and only once per book.
     *
     * @throws InvalidStateException if the member never returned this book
     * @throws InformationExistException if the member already reviewed this book
     */
    @Transactional
    public ReviewResponse create(Long bookId, ReviewRequest request, Authentication authentication) {
        String comment = validate(request);
        TokenUser user = getUser(authentication);
        Book book = getBook(bookId);
        if (!lendingClient.hasReturned(bookId)) {
            throw new InvalidStateException("You can only review books you have returned");
        }
        if (reviewRepository.existsByUserIdAndBookId(user.id(), bookId)) {
            throw new InformationExistException("You have already reviewed this book");
        }

        Review review = new Review();
        review.setUserId(user.id());
        review.setUsername(user.username());
        review.setBook(book);
        review.setRating(request.rating());
        review.setComment(comment);
        review = reviewRepository.save(review);
        return ReviewResponse.from(review, user.id());
    }

    @Transactional
    public ReviewResponse update(Long reviewId, ReviewRequest request, Authentication authentication) {
        String comment = validate(request);
        TokenUser user = getUser(authentication);
        Review review = getReview(reviewId);
        if (!review.getUserId().equals(user.id())) {
            throw new AccessDeniedException("You can only edit your own review");
        }
        review.setRating(request.rating());
        review.setComment(comment);
        return ReviewResponse.from(review, user.id());
    }

    /**
     * Deletes a review. Allowed for its author or a librarian.
     */
    @Transactional
    public void delete(Long reviewId, Authentication authentication) {
        TokenUser user = getUser(authentication);
        Review review = getReview(reviewId);
        if (!review.getUserId().equals(user.id()) && !Roles.isLibrarian(authentication)) {
            throw new AccessDeniedException("You can only delete your own review");
        }
        reviewRepository.delete(review);
    }

    /**
     * Checks that the rating is 1 to 10 and the comment is at most 1000 characters.
     *
     * @return the trimmed comment, or null when it is empty
     */
    private String validate(ReviewRequest request) {
        if (request == null || request.rating() == null
                || request.rating() < MIN_RATING || request.rating() > MAX_RATING) {
            throw new InvalidRequestException("Rating must be between " + MIN_RATING + " and " + MAX_RATING);
        }
        String comment = request.comment() == null ? "" : request.comment().trim();
        if (comment.length() > MAX_COMMENT_LENGTH) {
            throw new InvalidRequestException("Description can be at most " + MAX_COMMENT_LENGTH + " characters");
        }
        return comment.isEmpty() ? null : comment;
    }

    private TokenUser getUser(Authentication authentication) {
        return (TokenUser) authentication.getPrincipal();
    }

    private Book getBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Book with ID " + bookId + " not found"));
    }

    private Review getReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new InformationNotFoundException("Review with ID " + reviewId + " not found"));
    }
}
