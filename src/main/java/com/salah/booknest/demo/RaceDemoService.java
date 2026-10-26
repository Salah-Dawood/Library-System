package com.salah.booknest.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.LoanRequest;
import com.salah.booknest.model.response.LoanResponse;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.InventoryRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private static final int LOAN_LIMIT = 5;
    private static final List<LoanStatus> ACTIVE_LOANS = List.of(LoanStatus.BORROWED);

    private final InventoryRepository inventoryRepository;
    private final TransactionTemplate transactionTemplate;
    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper;

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

    /**
     * One member tries to borrow several different books at the same moment.
     * With safe=false the member row is not locked (how borrow worked before), so every thread passes the
     * 5 book check and the member ends up with more than 5 loans. With safe=true the real LoanService.borrow
     * is used, which locks the member first. The loans created here are removed again afterwards.
     */
    public LoanLimitRaceResult runLoanLimit(String username, int threads, boolean safe) throws InterruptedException {
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
        Long userId = user.getId();
        int loansBefore = countActiveLoans(userId);
        List<Long> bookIds = findFreeBooks(userId, threads);
        if (bookIds.size() < threads) {
            throw new InvalidStateException("Only " + bookIds.size() + " books with a free copy were found, " + threads + " are needed");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_member")));
        List<Long> createdLoanIds = new CopyOnWriteArrayList<>();
        int borrowed = 0;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch startSignal = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (Long bookId : bookIds) {
                results.add(pool.submit(() -> {
                    startSignal.await();
                    if (safe) {
                        return safeBorrow(authentication, bookId, createdLoanIds);
                    }
                    return unsafeBorrow(userId, bookId, createdLoanIds);
                }));
            }
            startSignal.countDown();

            for (Future<Boolean> result : results) {
                try {
                    if (result.get()) {
                        borrowed++;
                    }
                } catch (ExecutionException e) {
                    log.warn("A loan limit demo task failed: {}", e.getCause().toString());
                }
            }
        } finally {
            pool.shutdown();
        }

        int loansAfter = countActiveLoans(userId);
        removeDemoLoans(createdLoanIds);

        String verdict;
        if (loansAfter > LOAN_LIMIT) {
            verdict = "WRONG: the member ended up with " + loansAfter + " loans, the limit is " + LOAN_LIMIT;
        } else {
            verdict = "OK: the member has " + loansAfter + " loans, the limit of " + LOAN_LIMIT + " was respected";
        }
        log.info("Loan limit demo for {}: safe={}, threads={}, borrowed={}, loansAfter={}", username, safe, threads, borrowed, loansAfter);
        return new LoanLimitRaceResult(username, safe, threads, LOAN_LIMIT, loansBefore, borrowed, loansAfter, verdict);
    }

    // the real borrow: it locks the member first
    private boolean safeBorrow(Authentication authentication, Long bookId, List<Long> createdLoanIds) {
        try {
            LoanRequest request = objectMapper.convertValue(Map.of("bookId", bookId, "duration", 7), LoanRequest.class);
            LoanResponse loan = loanService.borrow(authentication, request);
            createdLoanIds.add(loan.id());
            return true;
        } catch (InvalidStateException e) {
            return false;
        }
    }

    // borrow without the member lock: it checks the limit, waits a moment, and only then takes the copy
    private boolean unsafeBorrow(Long userId, Long bookId, List<Long> createdLoanIds) {
        Boolean borrowed = transactionTemplate.execute(status -> {
            if (loanRepository.countByUserIdAndStatusIn(userId, ACTIVE_LOANS) >= LOAN_LIMIT) {
                return false;
            }
            pause();
            Inventory inventory = inventoryRepository.findByBookIdForUpdate(bookId).orElseThrow(() -> notFound(bookId));
            if (inventory.getAvailableCopies() < 1) {
                return false;
            }
            inventory.setAvailableCopies(inventory.getAvailableCopies() - 1);

            Loan loan = new Loan();
            loan.setUser(userRepository.getReferenceById(userId));
            loan.setBook(inventory.getBook());
            loan.setStatus(LoanStatus.BORROWED);
            loan.setRequestedDays(7);
            loan.setLoanDate(LocalDate.now());
            loan.setDueDate(LocalDate.now().plusDays(7));
            loan = loanRepository.save(loan);
            createdLoanIds.add(loan.getId());
            return true;
        });
        return Boolean.TRUE.equals(borrowed);
    }

    // books with a free copy that this member does not have yet
    private List<Long> findFreeBooks(Long userId, int count) {
        List<Long> bookIds = new ArrayList<>();
        transactionTemplate.executeWithoutResult(status -> {
            for (Book book : bookRepository.findAll()) {
                if (bookIds.size() == count) {
                    break;
                }
                Inventory inventory = book.getInventory();
                boolean free = Boolean.TRUE.equals(book.getActive()) && inventory != null && inventory.getAvailableCopies() >= 1;
                if (free && !loanRepository.existsByUserIdAndBookIdAndStatusIn(userId, book.getId(), ACTIVE_LOANS)) {
                    bookIds.add(book.getId());
                }
            }
        });
        return bookIds;
    }

    private int countActiveLoans(Long userId) {
        return (int) loanRepository.countByUserIdAndStatusIn(userId, ACTIVE_LOANS);
    }

    // deletes the loans made by the demo and puts their copies back
    private void removeDemoLoans(List<Long> loanIds) {
        for (Long loanId : loanIds) {
            transactionTemplate.executeWithoutResult(status -> {
                Optional<Loan> loan = loanRepository.findById(loanId);
                if (loan.isEmpty()) {
                    return;
                }
                Long bookId = loan.get().getBook().getId();
                loanRepository.delete(loan.get());
                Inventory inventory = inventoryRepository.findByBookIdForUpdate(bookId).orElseThrow(() -> notFound(bookId));
                inventory.setAvailableCopies(Math.min(inventory.getTotalCopies(), inventory.getAvailableCopies() + 1));
            });
        }
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

    // makes the race easy to see
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
