package com.salah.booknest.lending.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @param bookId      the book the stock belongs to (only used when the stock is created)
 * @param bookTitle   the title, copied from catalog-service so loans can show it (only used when the stock is created)
 * @param totalCopies how many copies the library owns
 */
public record InventoryRequest(
        Long bookId,
        String bookTitle,
        @NotNull(message = "Total copies can not be null") @Min(value = 0, message = "Total copies must be zero or more") Integer totalCopies) {
}
