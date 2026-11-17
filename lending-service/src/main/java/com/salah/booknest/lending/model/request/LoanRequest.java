package com.salah.booknest.lending.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
// the race demo builds requests in code
@AllArgsConstructor
public class LoanRequest {

    @NotNull
    private Long bookId;

    @NotNull
    private Integer duration;
}
