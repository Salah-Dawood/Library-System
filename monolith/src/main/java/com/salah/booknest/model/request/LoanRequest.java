package com.salah.booknest.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor//just for race demo
public class LoanRequest {

    @NotNull
    private Long bookId;

    @NotNull
    private Integer duration;
}
