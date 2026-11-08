package com.salah.booknest.catalog.repository;

import com.salah.booknest.catalog.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre,Long> {
    Optional<Genre> findByName(String name);
}
