package com.salah.booknest.model.request;

import lombok.Getter;

import java.util.List;

@Getter
public class CreateBookRequest {
    private String title;
    private String isbn;
    private int publishedYear;
    private Long authorId;
    private int totalCopies;
    private List<Long> genreIds;
}
