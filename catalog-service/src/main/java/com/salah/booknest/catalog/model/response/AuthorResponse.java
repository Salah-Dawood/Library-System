package com.salah.booknest.catalog.model.response;

import com.salah.booknest.catalog.model.Author;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {

    private Long id;
    private String name;
    private Boolean active;
    private Integer birthYear;
    private String nationality;

    public static AuthorResponse from(Author author) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getActive(),
                author.getBirthYear(),
                author.getNationality()
        );
    }
}