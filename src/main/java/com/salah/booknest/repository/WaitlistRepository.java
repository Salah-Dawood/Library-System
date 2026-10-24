package com.salah.booknest.repository;

import com.salah.booknest.model.WaitlistEntry;
import com.salah.booknest.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    boolean existsByUserIdAndBookIdAndStatusIn(Long userId, Long bookId, List<WaitlistStatus> statuses);

    long countByUserIdAndStatus(Long userId, WaitlistStatus status);

    Optional<WaitlistEntry> findFirstByBookIdAndStatusOrderByIdAsc(Long bookId, WaitlistStatus status);

    long countByBookIdAndStatus(Long bookId, WaitlistStatus status);

    long countByBookIdAndStatusAndIdLessThan(Long bookId, WaitlistStatus status, Long id);

    List<WaitlistEntry> findAllByUserIdAndStatusInOrderByIdAsc(Long userId, List<WaitlistStatus> statuses);

    List<WaitlistEntry> findAllByBookIdAndStatusInOrderByIdAsc(Long bookId, List<WaitlistStatus> statuses);

    List<WaitlistEntry> findAllByStatusAndHoldExpiresAtBefore(WaitlistStatus status, LocalDateTime time);

    List<WaitlistEntry> findAllByBookIdAndStatus(Long bookId, WaitlistStatus status);
}
