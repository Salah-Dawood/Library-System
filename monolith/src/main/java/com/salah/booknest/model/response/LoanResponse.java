package com.salah.booknest.model.response;

import com.salah.booknest.model.Fine;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.ReturnTiming;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long userId,
        String username,
        LoanStatus status,
        Integer requestedDays,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        LocalDateTime createdAt,
        ReturnTiming returnTiming,
        Long daysLate,
        boolean overdue,
        String reviewedBy,
        LocalDateTime reviewedAt,
        Boolean damaged,
        String reviewNotes,
        BigDecimal fineTotal) {

    public static LoanResponse from(Loan loan) {
        LocalDate today = LocalDate.now();
        boolean overdue = loan.getStatus() == LoanStatus.BORROWED
                && loan.getDueDate() != null && today.isAfter(loan.getDueDate());

        ReturnTiming timing = null;
        Long daysLate = null;
        if (loan.getStatus() == LoanStatus.RETURNED || loan.getStatus() == LoanStatus.PENDING_REVIEW) {
            daysLate = ReturnTiming.daysLate(loan.getDueDate(), loan.getReturnDate());
            timing = ReturnTiming.of(daysLate);
        } else if (overdue) {
            daysLate = ReturnTiming.daysLate(loan.getDueDate(), today);
        }

        // Fines are only looked up for finished loans, so list pages do not trigger an extra query per row
        BigDecimal fineTotal = null;
        if (loan.getStatus() == LoanStatus.RETURNED && loan.getFines() != null) {
            fineTotal = loan.getFines().stream().map(Fine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return new LoanResponse(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getUser().getId(),
                loan.getUser().getUsername(),
                loan.getStatus(),
                loan.getRequestedDays(),
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getCreatedAt(),
                timing,
                daysLate,
                overdue,
                loan.getReviewedBy() == null ? null : loan.getReviewedBy().getUsername(),
                loan.getReviewedAt(),
                loan.getDamaged(),
                loan.getReviewNotes(),
                fineTotal);
    }
}
