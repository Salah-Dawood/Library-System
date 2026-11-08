package com.salah.booknest.lending.model.response;

import com.salah.booknest.lending.model.LoanStatus;

import java.time.LocalDateTime;

public record NotificationEvent(
        String type,
        String message,
        Long loanId,
        LoanStatus status,
        LocalDateTime timestamp) {
}