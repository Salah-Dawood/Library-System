package com.salah.booknest.lending.repository;

import com.salah.booknest.lending.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    //prevent double booking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findWithLockByBookId(Long bookId);

    Optional<Inventory> findByBookId(Long bookId);

    Optional<Inventory> findByBookTitle(String bookTitle);

    List<Inventory> findAllByBookIdIn(Collection<Long> bookIds);
}