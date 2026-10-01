package com.salah.booknest.repository;

import com.salah.booknest.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre,Long> {
    Optional<Genre> findByName(String name);
}
