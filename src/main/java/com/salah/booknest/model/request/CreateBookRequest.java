package com.salah.booknest.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Getter
public class CreateBookRequest {

    @NotEmpty(message = "Title cannot be empty")
    private String title;

    @NotEmpty(message = "ISBN can not be empty")

    private String isbn;

    private Integer publishedYear;

    // published year validation
    @AssertTrue(message = "Published year hasn't happened yet!!")
    public boolean isPublishedYearValid() {
        int currentYear = Year.now().getValue();
        return this.publishedYear <= currentYear;
    }

    @NotNull(message = "Author ID is required")
    private Long authorId;

    @NotNull
    private Integer totalCopies;

    @NotEmpty
    private List<@NotNull Long> genreIds;
}
