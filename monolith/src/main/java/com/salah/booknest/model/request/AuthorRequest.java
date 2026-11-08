package com.salah.booknest.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AuthorRequest {
    @NotEmpty(message = "Author name can not be empty")
    private String name;

    @NotNull(message = "Author birth year can not be null")
    private Integer birthYear;

    @NotEmpty(message = "Author nationality can not be empty")
    private String nationality;
}
