package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.LoanRequest;
import com.salah.booknest.model.response.LoanResponse;
import com.salah.booknest.model.response.NotificationEvent;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.InventoryRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Loan workflow: a member requests a book, a librarian approves or rejects it,
 * and a copy is only taken from the inventory on approval.
 * <p>
 * Methods that change a loan lock its row, and always lock the loan before the inventory,
 * so concurrent requests are serialized and cannot deadlock or take the same copy twice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private static final int MAX_LOAN_DAYS = 30;
    private static final int MAX_ACTIVE_LOANS = 5;
    private static final List<LoanStatus> ACTIVE_STATUSES = List.of(LoanStatus.REQUESTED, LoanStatus.APPROVED);

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final NotificationService notificationService;

    // ---------- reads ----------

    /** @param status optional filter; {@code null} returns every loan */
    @Transactional(readOnly = true)
    public List<LoanResponse> getLoans(LoanStatus status) {
        List<Loan> loans = status == null
                ? loanRepository.findAllByOrderByCreatedAtDesc()
                : loanRepository.findAllByStatusOrderByCreatedAtDesc(status);
        return loans.stream().map(LoanResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getMyLoans(Authentication authentication) {
        return getByUserId(getUserFromAuth(authentication).getId());
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getByUserId(Long userId) {
        return loanRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(LoanResponse::from).toList();
    }

    // ---------- workflow ----------

    /** A member asks to borrow a book. No copy is reserved until a librarian approves. */
    @Transactional
    public LoanResponse requestLoan(Authentication authentication, LoanRequest request) {
        if (request.getBookId() == null) {
            throw new InvalidRequestException("Book ID is required");
        }
        Integer days = request.getDuration();
        if (days == null || days < 1 || days > MAX_LOAN_DAYS) {
            throw new InvalidRequestException("Loan duration must be between 1 and " + MAX_LOAN_DAYS + " days");
        }

        User user = getUserFromAuth(authentication);
        Book book = bookRepository.findById(request.getBookId()).orElseThrow(
                () -> new InformationNotFoundException("Book with ID " + request.getBookId() + " not found"));

        if (loanRepository.existsByUserIdAndBookIdAndStatusIn(user.getId(), book.getId(), ACTIVE_STATUSES)) {
            throw new InformationExistException("You already have an active request or loan for this book");
        }
        if (loanRepository.countByUserIdAndStatusIn(user.getId(), ACTIVE_STATUSES) >= MAX_ACTIVE_LOANS) {
            throw new InvalidStateException(
                    "You can have at most " + MAX_ACTIVE_LOANS + " active requests or loans");
        }

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setBook(book);
        loan.setRequestedDays(days);
        loan = loanRepository.save(loan);

        log.info("Loan {} requested by {} for book {}", loan.getId(), user.getUsername(), book.getId());
        notifyLibrarians(loan, "LOAN_REQUESTED", user.getUsername() + " requested \"" + book.getTitle() + "\"");
        return LoanResponse.from(loan);
    }

    /** Librarian approves: takes one copy from the inventory and sets the loan and due dates. */
    @Transactional
    public LoanResponse approve(Long loanId, Authentication librarian) {
        Loan loan = getLoanForUpdate(loanId);
        requireTransition(loan, LoanStatus.APPROVED);

        Inventory inventory = getInventoryForUpdate(loan.getBook().getId());
        if (inventory.getAvailableCopies() < 1) {
            throw new InvalidStateException("No copies of \"" + loan.getBook().getTitle() + "\" are available");
        }
        inventory.setAvailableCopies(inventory.getAvailableCopies() - 1);

        LocalDate today = LocalDate.now();
        loan.setStatus(LoanStatus.APPROVED);
        loan.setLoanDate(today);
        loan.setDueDate(today.plusDays(loan.getRequestedDays()));
        recordDecision(loan, librarian);

        log.info("Loan {} approved by {}", loanId, librarian.getName());
        notifyMember(loan, "LOAN_APPROVED",
                "Your request for \"" + loan.getBook().getTitle() + "\" was approved. Due " + loan.getDueDate());
        return LoanResponse.from(loan);
    }

    /** Librarian rejects a pending request, optionally with a reason. */
    @Transactional
    public LoanResponse reject(Long loanId, String reason, Authentication librarian) {
        Loan loan = getLoanForUpdate(loanId);
        requireTransition(loan, LoanStatus.REJECTED);

        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(reason == null || reason.isBlank() ? null : reason.trim());
        recordDecision(loan, librarian);

        log.info("Loan {} rejected by {}", loanId, librarian.getName());
        notifyMember(loan, "LOAN_REJECTED", "Your request for \"" + loan.getBook().getTitle() + "\" was rejected"
                + (loan.getRejectionReason() == null ? "" : ": " + loan.getRejectionReason()));
        return LoanResponse.from(loan);
    }

    /**
     * A member cancels their own pending request; a librarian can cancel any pending or approved loan.
     * Cancelling an approved loan gives its copy back.
     */
    @Transactional
    public LoanResponse cancel(Long loanId, Authentication authentication) {
        Loan loan = getLoanForUpdate(loanId);
        User actor = getUserFromAuth(authentication);
        boolean librarian = Roles.isLibrarian(authentication);
        boolean owner = loan.getUser().getId().equals(actor.getId());

        if (!owner && !librarian) {
            throw new AccessDeniedException("You can only cancel your own loan requests");
        }
        requireTransition(loan, LoanStatus.CANCELLED);
        if (loan.getStatus() == LoanStatus.APPROVED) {
            if (!librarian) {
                throw new AccessDeniedException("Only a librarian can cancel an approved loan");
            }
            giveCopyBack(loan.getBook().getId());
        }

        loan.setStatus(LoanStatus.CANCELLED);
        log.info("Loan {} cancelled by {}", loanId, authentication.getName());
        if (owner) {
            notifyLibrarians(loan, "LOAN_CANCELLED",
                    actor.getUsername() + " cancelled the request for \"" + loan.getBook().getTitle() + "\"");
        } else {
            recordDecision(loan, authentication);
            notifyMember(loan, "LOAN_CANCELLED",
                    "Your loan of \"" + loan.getBook().getTitle() + "\" was cancelled by a librarian");
        }
        return LoanResponse.from(loan);
    }

    /** Librarian records that the book came back; its copy returns to the inventory. */
    @Transactional
    public LoanResponse returnLoan(Long loanId, Authentication librarian) {
        Loan loan = getLoanForUpdate(loanId);
        requireTransition(loan, LoanStatus.RETURNED);

        giveCopyBack(loan.getBook().getId());
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDate.now());

        log.info("Loan {} returned, recorded by {}", loanId, librarian.getName());
        notifyMember(loan, "LOAN_RETURNED", "\"" + loan.getBook().getTitle() + "\" was marked as returned");
        return LoanResponse.from(loan);
    }

    // ---------- helpers ----------

    private User getUserFromAuth(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
    }

    private Loan getLoanForUpdate(Long loanId) {
        return loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new InformationNotFoundException("Loan with ID " + loanId + " not found"));
    }

    private Inventory getInventoryForUpdate(Long bookId) {
        return inventoryRepository.findByBookIdForUpdate(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Inventory for book " + bookId + " not found"));
    }

    private void requireTransition(Loan loan, LoanStatus next) {
        if (!loan.getStatus().canChangeTo(next)) {
            throw new InvalidStateException(
                    "A loan that is " + loan.getStatus() + " cannot be changed to " + next);
        }
    }

    private void giveCopyBack(Long bookId) {
        Inventory inventory = getInventoryForUpdate(bookId);
        // Never exceed the stock, even if the data was edited by hand.
        inventory.setAvailableCopies(Math.min(inventory.getTotalCopies(), inventory.getAvailableCopies() + 1));
    }

    private void recordDecision(Loan loan, Authentication librarian) {
        loan.setDecidedBy(getUserFromAuth(librarian));
        loan.setDecidedAt(LocalDateTime.now());
    }

    private void notifyMember(Loan loan, String type, String message) {
        String username = loan.getUser().getUsername();
        NotificationEvent event = newEvent(loan, type, message);
        afterCommit(() -> notificationService.notifyUser(username, event));
    }

    private void notifyLibrarians(Loan loan, String type, String message) {
        NotificationEvent event = newEvent(loan, type, message);
        afterCommit(() -> notificationService.notifyLibrarians(event));
    }

    private NotificationEvent newEvent(Loan loan, String type, String message) {
        return new NotificationEvent(type, message, loan.getId(), loan.getStatus(), LocalDateTime.now());
    }

    /** Pushes events only after the database commit, so clients never hear about a change that was rolled back. */
    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}