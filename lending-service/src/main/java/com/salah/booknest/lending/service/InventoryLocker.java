package com.salah.booknest.lending.service;

import com.salah.booknest.lending.exception.InformationNotFoundException;
import com.salah.booknest.lending.model.Inventory;
import com.salah.booknest.lending.repository.InventoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryLocker {

    private final InventoryRepository inventoryRepository;
    private final EntityManager entityManager;

    public Inventory lock(Long bookId) {
        Inventory inventory = inventoryRepository.findByBookIdForUpdate(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Inventory for book " + bookId + " not found"));
        // the row may already be loaded with old numbers, so read it again after the lock
        entityManager.refresh(inventory);
        return inventory;
    }
}
