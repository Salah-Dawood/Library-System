package com.salah.booknest.repository;

import com.salah.booknest.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author,Long> {
    Boolean existsByName(String name);
    Optional<Author> findByName(String name);
    Optional<Author> findByIdAndActiveTrue(Long id);
    Optional<Author> findByNameAndActiveTrue(String name);
    List<Author> findAllByActiveTrue();
}
