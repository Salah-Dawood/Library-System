package com.salah.booknest.model.request;

import com.salah.booknest.model.User;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LoanRequest {

    @NotNull
    private Long bookId;

    @NotNull
    private Integer duration;
}
