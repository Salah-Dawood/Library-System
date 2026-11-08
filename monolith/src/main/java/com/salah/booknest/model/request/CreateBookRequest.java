package com.salah.booknest.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import org.hibernate.validator.constraints.ISBN;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Getter
public class CreateBookRequest {

    @NotEmpty(message = "Title cannot be empty")
    private String title;

    @NotEmpty(message = "ISBN can not be empty")

    @ISBN(message = "Not a Valid ISBN")
    private String isbn;

    @NotNull(message = "Year can not be null")
    @Max(value = 2026,message = "Year hasn't happened yet, refer to Time travel section.")
    private Integer publishedYear;

    @NotNull(message = "Author ID is required")
    private Long authorId;

    @NotNull(message = "Total copies can not be null")
    private Integer totalCopies;

    @NotEmpty(message = "Please add at least one genre")
    private List<@NotNull Long> genreIds;
}
