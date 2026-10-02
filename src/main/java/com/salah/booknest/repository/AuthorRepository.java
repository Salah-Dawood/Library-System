package com.salah.booknest.repository;

import com.salah.booknest.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author,Long> {
    Boolean existsByName(String name);
    Optional<Author> findByName(String name);
}
