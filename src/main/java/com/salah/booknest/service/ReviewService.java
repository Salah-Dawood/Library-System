package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.Review;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.ReviewRequest;
import com.salah.booknest.model.response.ReviewResponse;
import com.salah.booknest.model.response.ReviewSummary;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.ReviewRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.security.Roles;
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
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public ReviewSummary getBookReviews(Long bookId, Authentication authentication) {
        getBook(bookId);
        User user = getUser(authentication);
        List<Review> reviews = reviewRepository.findAllByBookIdOrderByCreatedAtDesc(bookId);

        Long myReviewId = reviews.stream()
                .filter(review -> review.getUser().getId().equals(user.getId()))
                .map(Review::getId).findFirst().orElse(null);
        Double average = reviews.isEmpty() ? null
                : Math.round(reviews.stream().mapToInt(Review::getRating).average().orElse(0) * 10) / 10.0;

        return new ReviewSummary(
                reviews.stream().map(review -> ReviewResponse.from(review, user.getId())).toList(),
                average,
                reviews.size(),
                myReviewId == null && hasReturned(user.getId(), bookId),
                myReviewId);
    }

    @Transactional
    public ReviewResponse create(Long bookId, ReviewRequest request, Authentication authentication) {
        String comment = validate(request);
        User user = getUser(authentication);
        Book book = getBook(bookId);

        if (!hasReturned(user.getId(), bookId)) {
            throw new InvalidStateException("You can only review books you have returned");
        }
        if (reviewRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            throw new InformationExistException("You have already reviewed this book");
        }

        Review review = new Review();
        review.setUser(user);
        review.setBook(book);
        review.setRating(request.rating());
        review.setComment(comment);
        review = reviewRepository.save(review);

        auditLogService.log("REVIEW", "CREATED", review.getId());
        return ReviewResponse.from(review, user.getId());
    }

    @Transactional
    public ReviewResponse update(Long reviewId, ReviewRequest request, Authentication authentication) {
        String comment = validate(request);
        User user = getUser(authentication);
        Review review = getReview(reviewId);
        if (!review.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only edit your own review");
        }

        review.setRating(request.rating());
        review.setComment(comment);
        auditLogService.log("REVIEW", "UPDATED", reviewId);
        return ReviewResponse.from(review, user.getId());
    }

    @Transactional
    public void delete(Long reviewId, Authentication authentication) {
        User user = getUser(authentication);
        Review review = getReview(reviewId);
        if (!review.getUser().getId().equals(user.getId()) && !Roles.isLibrarian(authentication)) {
            throw new AccessDeniedException("You can only delete your own review");
        }
        //Audited first so the entry can still name the review it is about.
        auditLogService.log("REVIEW", "DELETED", reviewId);
        reviewRepository.delete(review);
    }

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

    private boolean hasReturned(Long userId, Long bookId) {
        return loanRepository.existsByUserIdAndBookIdAndStatusIn(userId, bookId, List.of(LoanStatus.RETURNED));
    }

    private User getUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
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
