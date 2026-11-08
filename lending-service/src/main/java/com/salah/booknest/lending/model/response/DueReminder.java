package com.salah.booknest.lending.model.response;

import java.time.LocalDate;

public record DueReminder(String username, String email, String bookTitle, LocalDate dueDate) {
}
