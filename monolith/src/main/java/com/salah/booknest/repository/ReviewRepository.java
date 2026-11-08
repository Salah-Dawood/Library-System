package com.salah.booknest.repository;

import com.salah.booknest.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findAllByBookIdOrderByCreatedAtDesc(Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    long countByBookId(Long bookId);

    @Query("select avg(r.rating) from Review r where r.book.id = :bookId")
    Double averageRatingByBookId(@Param("bookId") Long bookId);
}
