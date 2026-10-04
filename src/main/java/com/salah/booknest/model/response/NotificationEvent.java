package com.salah.booknest.model.response;

import com.salah.booknest.model.LoanStatus;

import java.time.LocalDateTime;

public record NotificationEvent(
        String type,
        String message,
        Long loanId,
        LoanStatus status,
        LocalDateTime timestamp) {
}