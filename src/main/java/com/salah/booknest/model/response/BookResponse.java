package com.salah.booknest.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BookResponse {
    private Long id;
    private String title;
    private String isbn;
    private int publishedYear;
    private int totalCopies;
    private int availableCopies;
    private Double averageRating;
    private int reviewCount;
    private String authorName;
    private List<String> genreNames;
}
