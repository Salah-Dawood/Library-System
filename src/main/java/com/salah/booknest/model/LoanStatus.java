package com.salah.booknest.model;

import java.util.Set;


/**
 * BORROWED -> PENDING_REVIEW -> RETURNED, or BORROWED -> CANCELLED.
 * Borrowing is instant (no approval), and every return waits for a librarian's damage review.
 */
public enum LoanStatus {
    /** The member has the book. Starts here as soon as they borrow it. */
    BORROWED,
    /** The member handed the book back; a librarian still has to check it for damage. */
    PENDING_REVIEW,
    /** The librarian reviewed the return and the copy is dealt with. */
    RETURNED,
    /** A librarian cancelled the loan while it was out. */
    CANCELLED;

    private Set<LoanStatus> allowedNext() {
        return switch (this) {
            case BORROWED -> Set.of(PENDING_REVIEW, CANCELLED);
            case PENDING_REVIEW -> Set.of(RETURNED);
            case RETURNED, CANCELLED -> Set.of();
        };
    }

    public boolean canChangeTo(LoanStatus next) {
        return allowedNext().contains(next);
    }
}