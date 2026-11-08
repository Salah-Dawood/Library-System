package com.salah.booknest.catalog.model.response;

/** Copy counts of one book, as sent by lending-service. */
public record StockResponse(Long bookId, int totalCopies, int availableCopies) {
}
