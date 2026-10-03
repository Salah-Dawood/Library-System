package com.salah.booknest.model.request;

import com.salah.booknest.model.User;
import lombok.Getter;

@Getter
public class LoanRequest {
    private Long bookId;
    private Integer duration;
}
