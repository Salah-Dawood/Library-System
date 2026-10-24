package com.salah.booknest.repository;

import com.salah.booknest.model.WaitlistEntry;
import com.salah.booknest.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, WaitlistStatus status);

    long countByUserIdAndStatus(Long userId, WaitlistStatus status);

    Optional<WaitlistEntry> findFirstByBookIdAndStatusOrderByIdAsc(Long bookId, WaitlistStatus status);

    long countByBookIdAndStatus(Long bookId, WaitlistStatus status);

    long countByBookIdAndStatusAndIdLessThan(Long bookId, WaitlistStatus status, Long id);

    List<WaitlistEntry> findAllByUserIdAndStatusOrderByIdAsc(Long userId, WaitlistStatus status);

    List<WaitlistEntry> findAllByBookIdAndStatusOrderByIdAsc(Long bookId, WaitlistStatus status);

    List<WaitlistEntry> findAllByBookIdAndStatus(Long bookId, WaitlistStatus status);
}
