package com.salah.booknest.lending.service;

import com.salah.booknest.lending.exception.InformationExistException;
import com.salah.booknest.lending.exception.InformationNotFoundException;
import com.salah.booknest.lending.exception.InvalidRequestException;
import com.salah.booknest.lending.exception.InvalidStateException;
import com.salah.booknest.lending.model.Inventory;
import com.salah.booknest.lending.model.LoanStatus;
import com.salah.booknest.lending.model.request.InventoryRequest;
import com.salah.booknest.lending.model.response.InventoryResponse;
import com.salah.booknest.lending.repository.InventoryRepository;
import com.salah.booknest.lending.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The copy counts of every book. The catalog asks this service for stock instead of reading it itself.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final LoanRepository loanRepository;
    private final InventoryLocker inventoryLocker;
    private final WaitlistService waitlistService;

    @Transactional(readOnly = true)
    public List<InventoryResponse> getStock(List<Long> bookIds) {
        return inventoryRepository.findAllByBookIdIn(bookIds).stream().map(InventoryResponse::from).toList();
    }

    @Transactional
    public InventoryResponse createStock(InventoryRequest request) {
        if (request.bookId() == null) {
            throw new InvalidRequestException("Book ID must be provided");
        }
        if (inventoryRepository.findByBookId(request.bookId()).isPresent()) {
            throw new InformationExistException("Stock for book " + request.bookId() + " already exists");
        }

        Inventory inventory = new Inventory();
        inventory.setBookId(request.bookId());
        inventory.setBookTitle(request.bookTitle());
        inventory.setTotalCopies(request.totalCopies());
        inventory.setAvailableCopies(request.totalCopies());
        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    /**
     * Changes the total number of copies while keeping the copies currently on loan unchanged:
     * available copies become the new total minus the copies on loan. Every new copy is offered to the
     * waitlist first, so people in line are not skipped.
     *
     * @throws InvalidRequestException if the new total is lower than the number of copies on loan
     */
    @Transactional
    public InventoryResponse changeTotalCopies(Long bookId, int newTotal) {
        Inventory inventory = inventoryLocker.lock(bookId);
        int oldTotal = inventory.getTotalCopies();
        int oldAvailable = inventory.getAvailableCopies();
        int onLoan = oldTotal - oldAvailable;
        if (newTotal < onLoan) {
            throw new InvalidRequestException(
                    "Total copies cannot be lower than the " + onLoan + " copies currently on loan");
        }
        inventory.setTotalCopies(newTotal);
        if (newTotal > oldTotal) {
            inventory.setAvailableCopies(oldAvailable);
            for (int i = oldTotal; i < newTotal; i++) {
                waitlistService.offerCopy(inventory);
            }
        } else {
            inventory.setAvailableCopies(newTotal - onLoan);
        }
        log.info("Stock of book {} changed: total {} -> {}, available {} -> {}",
                bookId, oldTotal, newTotal, oldAvailable, inventory.getAvailableCopies());
        return InventoryResponse.from(inventory);
    }

    /**
     * Takes a book out of lending: refused while a copy is on loan or a return is waiting for review,
     * otherwise everyone waiting for it is removed from the waitlist and the book can no longer be borrowed.
     */
    @Transactional
    public void retire(Long bookId) {
        boolean inUse = loanRepository.existsByBookIdAndStatusIn(
                bookId, List.of(LoanStatus.BORROWED, LoanStatus.PENDING_REVIEW));
        if (inUse) {
            throw new InvalidStateException("Book is on loan or has returns waiting for review and cannot be removed");
        }
        waitlistService.cancelAllForBook(bookId);
        inventoryLocker.lock(bookId).setActive(false);
    }
}
