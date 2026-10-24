package com.salah.booknest.repository;

import com.salah.booknest.model.WaitlistEntry;
import com.salah.booknest.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, WaitlistStatus status);

    long countByUserIdAndStatus(Long userId, WaitlistStatus status);
}
