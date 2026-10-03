package com.salah.booknest.model.response;

import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** What the API returns for a loan. Flat on purpose: no nested User or Book entities. */
public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String username,
        LoanStatus status,
        Integer requestedDays,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        String decidedBy,
        LocalDateTime decidedAt,
        String rejectionReason,
        LocalDateTime createdAt) {

    /** Must be called while the Hibernate session is open, because user and book are lazy. */
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getUser().getUsername(),
                loan.getStatus(),
                loan.getRequestedDays(),
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getDecidedBy() == null ? null : loan.getDecidedBy().getUsername(),
                loan.getDecidedAt(),
                loan.getRejectionReason(),
                loan.getCreatedAt());
    }
}