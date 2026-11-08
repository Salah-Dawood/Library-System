package com.salah.booknest.catalog.repository;

import com.salah.booknest.catalog.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book,Long> {
    Optional<Book> findByTitle(String title);
    Optional<Book> findByTitleAndActiveTrue(String title);
    Optional<Book> findByIdAndActiveTrue(Long id);
    Optional<Book> findByIsbn(String isbn);

    Page<Book> findAll(Specification<Book> spec, Pageable pageable);
}
