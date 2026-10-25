package com.salah.booknest.demo;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
@Profile("race-demo")
@RequiredArgsConstructor
public class RaceDemoService {

    private final InventoryRepository inventoryRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * Sets the book to one copy, then lets many threads try to take it at the same moment.
     * With safe=false nobody locks the inventory, so several threads can take the same last copy.
     * With safe=true the inventory row is locked, like LoanService.borrow does.
     * The original stock is restored at the end.
     */
    public RaceResult run(Long bookId, int threads, boolean safe) throws InterruptedException {
        int originalTotal = readTotal(bookId);
        int originalAvailable = readAvailable(bookId);
        setStock(bookId, 1, 1);

        int taken = 0;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch startSignal = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    startSignal.await();
                    return transactionTemplate.execute(status -> takeCopy(bookId, safe));
                }));
            }
            startSignal.countDown();

            for (Future<Boolean> result : results) {
                try {
                    if (result.get()) {
                        taken++;
                    }
                } catch (ExecutionException e) {
                    log.error("A race demo task failed", e);
                }
            }
        } finally {
            pool.shutdown();
        }

        int availableAfter = readAvailable(bookId);
        setStock(bookId, originalTotal, originalAvailable);

        String verdict = taken > 1
                ? "WRONG: " + taken + " members got the only copy"
                : "OK: only one member got the only copy";
        log.info("Race demo on book {}: safe={}, threads={}, taken={}", bookId, safe, threads, taken);
        return new RaceResult(safe, threads, 1, taken, availableAfter, verdict);
    }

    private boolean takeCopy(Long bookId, boolean safe) {
        Inventory inventory;
        if (safe) {
            inventory = inventoryRepository.findByBookIdForUpdate(bookId).orElseThrow(() -> notFound(bookId));
        } else {
            inventory = inventoryRepository.findByBookId(bookId).orElseThrow(() -> notFound(bookId));
        }
        if (inventory.getAvailableCopies() < 1) {
            return false;
        }

        pause();
        inventory.setAvailableCopies(inventory.getAvailableCopies() - 1);
        return true;
    }

    private void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int readTotal(Long bookId) {
        return inventoryRepository.findByBookId(bookId).orElseThrow(() -> notFound(bookId)).getTotalCopies();
    }

    private int readAvailable(Long bookId) {
        return inventoryRepository.findByBookId(bookId).orElseThrow(() -> notFound(bookId)).getAvailableCopies();
    }

    private void setStock(Long bookId, int total, int available) {
        transactionTemplate.executeWithoutResult(status -> {
            Inventory inventory = inventoryRepository.findByBookId(bookId).orElseThrow(() -> notFound(bookId));
            inventory.setTotalCopies(total);
            inventory.setAvailableCopies(available);
        });
    }

    private InformationNotFoundException notFound(Long bookId) {
        return new InformationNotFoundException("Inventory for book " + bookId + " not found");
    }
}
