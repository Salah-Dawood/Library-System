package com.salah.booknest.model.response;

import com.salah.booknest.model.Inventory;

public record InventoryResponse(Long bookId, int totalCopies, int availableCopies) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(inventory.getBook().getId(), inventory.getTotalCopies(), inventory.getAvailableCopies());
    }
}
