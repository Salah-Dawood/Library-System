package com.salah.booknest.repository;

import com.salah.booknest.model.Fine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Fine> findWithLockById(Long id);
}
