package com.salah.booknest.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String authorName;
    private List<String> genreNames;
}
