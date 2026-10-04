package com.salah.booknest.model.response;

import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.ReturnTiming;

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
        String decidedBy,
        LocalDateTime decidedAt,
        String rejectionReason,
        LocalDateTime createdAt,
        ReturnTiming returnTiming,
        Long daysLate,
        boolean overdue) {

    public static LoanResponse from(Loan loan) {
        LocalDate today = LocalDate.now();
        boolean overdue = loan.getStatus() == LoanStatus.APPROVED
                && loan.getDueDate() != null && today.isAfter(loan.getDueDate());

        ReturnTiming timing = null;
        Long daysLate = null;
        if (loan.getStatus() == LoanStatus.RETURNED) {
            daysLate = ReturnTiming.daysLate(loan.getDueDate(), loan.getReturnDate());
            timing = ReturnTiming.of(daysLate);
        } else if (overdue) {
            daysLate = ReturnTiming.daysLate(loan.getDueDate(), today);
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
                loan.getDecidedBy() == null ? null : loan.getDecidedBy().getUsername(),
                loan.getDecidedAt(),
                loan.getRejectionReason(),
                loan.getCreatedAt(),
                timing,
                daysLate,
                overdue);
    }
}
