package com.salah.booknest.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

@Getter
public class GenreRequest {

    @NotEmpty(message = "Genre name can not be empty")
    private String name;

    @NotEmpty(message = "Genre description can not be empty")
    private String description;
}
