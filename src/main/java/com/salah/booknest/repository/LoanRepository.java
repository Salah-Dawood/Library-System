package com.salah.booknest.repository;

import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Page<Loan> findAllByStatus(LoanStatus status, Pageable pageable);

    Page<Loan> findAllByUserId(Long userId, Pageable pageable);

    List<Loan> findAllByUserIdAndStatus(Long userId, LoanStatus status);

    boolean existsByUserIdAndBookIdAndStatusIn(Long userId, Long bookId, Collection<LoanStatus> statuses);

    boolean existsByBookIdAndStatusIn(Long bookId, Collection<LoanStatus> statuses);

    long countByUserIdAndStatusIn(Long userId, Collection<LoanStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);
}
