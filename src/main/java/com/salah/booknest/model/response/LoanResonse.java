package com.salah.booknest.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class LoanResonse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String username;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
}
