package com.salah.booknest.model.request;

import lombok.Getter;

import java.util.List;

@Getter
public class CreateBookRequest {
    private String title;
    private String isbn;
    private Integer publishedYear;
    private Long authorId;
    private Integer totalCopies;
    private List<Long> genreIds;
}
