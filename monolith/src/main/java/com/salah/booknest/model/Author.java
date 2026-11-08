package com.salah.booknest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    @ColumnDefault("true")
    private Boolean active = true;

    @Column(nullable = false)
    private Integer birthYear;

    @Column(nullable = false)
    private String nationality;

    @OneToMany(mappedBy = "author")
    @JsonIgnoreProperties("author")
    private List<Book> books = new ArrayList<>();
}
