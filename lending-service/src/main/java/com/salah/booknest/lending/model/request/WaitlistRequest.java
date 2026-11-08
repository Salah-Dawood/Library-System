package com.salah.booknest.lending.model.request;

import jakarta.validation.constraints.NotNull;

public record WaitlistRequest(@NotNull(message = "Book ID can not be null!") Long bookId) {
}
