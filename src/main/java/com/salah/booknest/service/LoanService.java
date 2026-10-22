package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Fine;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.ReturnTiming;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.LoanRequest;
import com.salah.booknest.model.request.ReviewReturnRequest;
import com.salah.booknest.model.response.LoanResponse;
import com.salah.booknest.model.response.NotificationEvent;
import com.salah.booknest.model.response.ReturnStats;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.FineRepository;
import com.salah.booknest.repository.InventoryRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.security.Roles;
import com.salah.booknest.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final int MAX_LOAN_DAYS = 30;
    private static final int MAX_ACTIVE_LOANS = 5;
    private static final int MIN_RETURNS_FOR_BADGE = 3;
    private static final int RELIABLE_RATE_PERCENT = 90;
    private static final List<LoanStatus> ACTIVE_STATUSES = List.of(LoanStatus.BORROWED);
    private static final List<LoanStatus> RETURN_STATUSES = List.of(LoanStatus.PENDING_REVIEW, LoanStatus.RETURNED);
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "createdAt", "loanDate", "dueDate", "returnDate", "status");

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final FineRepository fineRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;


    /**
     * Returns one page of all loans, optionally only those with the given status.
     * Page size is limited to 100.
     *
     * @throws InvalidRequestException if sortBy is not an allowed field
     */
    @Transactional(readOnly = true)
    public Page<LoanResponse> getLoans(LoanStatus status, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageUtils.pageable(page, size, sortBy, sortDir, SORTABLE_FIELDS);
        Page<Loan> loans = status == null
                ? loanRepository.findAll(pageable)
                : loanRepository.findAllByStatus(status, pageable);
        return loans.map(LoanResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getMyLoans(Authentication authentication, int page, int size, String sortBy, String sortDir) {
        return getByUserId(getUserFromAuth(authentication).getId(), page, size, sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getByUserId(Long userId, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageUtils.pageable(page, size, sortBy, sortDir, SORTABLE_FIELDS);
        return loanRepository.findAllByUserId(userId, pageable).map(LoanResponse::from);
    }

    //workflow

    /**
     * Borrows a book on the spot for the logged-in member, with no approval step.
     * One copy is taken out of stock right away, the loan date is today and the due date is today
     * plus the requested days. The inventory row is locked so two members cannot take the last copy.
     *
     * @throws InvalidRequestException if the book id is missing or the duration is not 1 to 30 days
     * @throws InformationExistException if the member already has this book on loan
     * @throws InvalidStateException if the book is inactive, no copies are available,
     *                               or the member already has 5 books on loan
     */
    @Transactional
    public LoanResponse borrow(Authentication authentication, LoanRequest request) {
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
        if (!Boolean.TRUE.equals(book.getActive())) {
            throw new InvalidStateException("This book is no longer available");
        }

        if (loanRepository.existsByUserIdAndBookIdAndStatusIn(user.getId(), book.getId(), ACTIVE_STATUSES)) {
            throw new InformationExistException("You already have this book on loan");
        }
        if (loanRepository.countByUserIdAndStatusIn(user.getId(), ACTIVE_STATUSES) >= MAX_ACTIVE_LOANS) {
            throw new InvalidStateException("You can have at most " + MAX_ACTIVE_LOANS + " books on loan");
        }

        Inventory inventory = getInventoryForUpdate(book.getId());
        if (inventory.getAvailableCopies() < 1) {
            throw new InvalidStateException("No copies of \"" + book.getTitle() + "\" are available");
        }
        inventory.setAvailableCopies(inventory.getAvailableCopies() - 1);

        LocalDate today = LocalDate.now();
        Loan loan = new Loan();
        loan.setUser(user);
        loan.setBook(book);
        loan.setStatus(LoanStatus.BORROWED);
        loan.setRequestedDays(days);
        loan.setLoanDate(today);
        loan.setDueDate(today.plusDays(days));
        loan = loanRepository.save(loan);

        auditLogService.log("LOAN", "BORROWED", loan.getId());
        return LoanResponse.from(loan);
    }


    /**
     * Cancels a loan that is still out (for example one made by mistake) and puts the copy back in stock.
     * Librarian only.
     *
     * @throws InvalidStateException if the loan is already finished (returned or cancelled)
     */
    @Transactional
    public LoanResponse cancel(Long loanId) {
        Loan loan = getLoanForUpdate(loanId);
        requireTransition(loan, LoanStatus.CANCELLED);
        giveCopyBack(loan.getBook().getId());

        loan.setStatus(LoanStatus.CANCELLED);
        auditLogService.log("LOAN", "CANCELLED", loanId);
        notifyMember(loan, "LOAN_CANCELLED",
                "Your loan of \"" + loan.getBook().getTitle() + "\" was cancelled by a librarian");
        return LoanResponse.from(loan);
    }


    /**
     * Hands a loan back today. The return is checked against the due date (early, on time or late)
     * and the loan moves to PENDING_REVIEW, where a librarian inspects the copy for damage.
     * The copy is NOT put back in stock yet; that happens when the librarian clears it.
     * Allowed for the borrower or a librarian.
     *
     * @throws AccessDeniedException if the caller is neither the borrower nor a librarian
     * @throws InvalidStateException if the loan is not currently borrowed
     */
    @Transactional
    public LoanResponse returnLoan(Long loanId, Authentication authentication) {
        Loan loan = getLoanForUpdate(loanId);
        boolean owner = loan.getUser().getUsername().equals(authentication.getName());
        if (!owner && !Roles.isLibrarian(authentication)) {
            throw new AccessDeniedException("You can only return your own loans");
        }
        requireTransition(loan, LoanStatus.PENDING_REVIEW);

        loan.setStatus(LoanStatus.PENDING_REVIEW);
        loan.setReturnDate(LocalDate.now());

        String timing = describeTiming(ReturnTiming.daysLate(loan.getDueDate(), loan.getReturnDate()));
        auditLogService.log("LOAN", "RETURNED", loanId);

        String title = loan.getBook().getTitle();
        if (owner) {
            notifyLibrarians(loan, "LOAN_RETURNED",
                    loan.getUser().getUsername() + " returned \"" + title + "\" " + timing + ". Waiting for review");
        } else {
            notifyMember(loan, "LOAN_RETURNED",
                    "\"" + title + "\" was marked as returned " + timing + ". It is waiting for a librarian's review");
        }
        return LoanResponse.from(loan);
    }


    /**
     * Returns one page of loans waiting for a return review (PENDING_REVIEW), oldest return first by default.
     * Each entry shows whether it came back early, on time or late.
     *
     * @throws InvalidRequestException if sortBy is not an allowed field
     */
    @Transactional(readOnly = true)
    public Page<LoanResponse> getReturnsReview(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageUtils.pageable(page, size, sortBy, sortDir, SORTABLE_FIELDS);
        return loanRepository.findAllByStatus(LoanStatus.PENDING_REVIEW, pageable).map(LoanResponse::from);
    }

    /**
     * Finishes a return after the librarian has inspected the copy.
     * Not damaged: the copy goes back in stock. Damaged: the copy is taken out of circulation
     * (total copies goes down by one). Either way a fine can be charged, and the member is told the outcome.
     *
     * @throws InvalidStateException if the loan is not waiting for review
     */
    @Transactional
    public LoanResponse reviewReturn(Long loanId, ReviewReturnRequest request, Authentication librarian) {
        Loan loan = getLoanForUpdate(loanId);
        if (loan.getStatus() != LoanStatus.PENDING_REVIEW) {
            throw new InvalidStateException("Only a returned loan waiting for review can be reviewed, but this loan is "
                    + loan.getStatus());
        }

        String notes = request.notes() == null || request.notes().isBlank() ? null : request.notes().trim();
        long daysLate = ReturnTiming.daysLate(loan.getDueDate(), loan.getReturnDate());
        boolean late = ReturnTiming.of(daysLate) == ReturnTiming.LATE;

        if (request.damaged()) {
            withdrawCopy(loan.getBook().getId());
        } else {
            giveCopyBack(loan.getBook().getId());
        }

        BigDecimal fineAmount = request.fineAmount();
        boolean fined = fineAmount != null && fineAmount.signum() > 0;
        if (fined) {
            Fine fine = new Fine();
            fine.setLoan(loan);
            fine.setAmount(fineAmount);
            fine.setReason(fineReason(request.damaged(), late, daysLate, notes));
            fineRepository.save(fine);
        }

        loan.setStatus(LoanStatus.RETURNED);
        loan.setDamaged(request.damaged());
        loan.setReviewNotes(notes);
        loan.setReviewedBy(getUserFromAuth(librarian));
        loan.setReviewedAt(LocalDateTime.now());

        auditLogService.log("LOAN", request.damaged() ? "RETURN_DAMAGED" : "RETURN_CLEARED", loanId);

        String title = loan.getBook().getTitle();
        String message = request.damaged()
                ? "Your return of \"" + title + "\" was reviewed: damage was found" + (notes == null ? "" : " (" + notes + ")")
                : "Your return of \"" + title + "\" was reviewed and accepted. Thank you!";
        if (fined) {
            message += ". A fine of " + fineAmount.toPlainString() + " was added";
        }
        notifyMember(loan, "RETURN_REVIEWED", message);
        return LoanResponse.from(loan);
    }


    @Transactional(readOnly = true)
    public ReturnStats getMyStats(Authentication authentication) {
        return getStats(getUserFromAuth(authentication).getId());
    }

    //count return status
    /**
     * Summarises how a user returns books: counts of early, on-time and late returns and the
     * percentage returned on time or early. The reliable badge needs at least 3 returns and 90% or more on time.
     */
    @Transactional(readOnly = true)
    public ReturnStats getStats(Long userId) {
        int early = 0;
        int onTime = 0;
        int late = 0;
        List<Loan> returned = loanRepository.findAllByUserIdAndStatusIn(userId, RETURN_STATUSES);
        for (Loan loan : returned) {
            switch (ReturnTiming.of(ReturnTiming.daysLate(loan.getDueDate(), loan.getReturnDate()))) {
                case EARLY -> early++;
                case ON_TIME -> onTime++;
                case LATE -> late++;
            }
        }
        int total = returned.size();
        int rate = total == 0 ? 0 : Math.round(100f * (early + onTime) / total);
        return new ReturnStats(total, early, onTime, late, rate,
                total >= MIN_RETURNS_FOR_BADGE && rate >= RELIABLE_RATE_PERCENT);
    }

    // helper methods

    private User getUserFromAuth(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
    }

    /**
     * Loads a loan with a database write lock so concurrent status changes on the same loan are serialised.
     */
    private Loan getLoanForUpdate(Long loanId) {
        return loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new InformationNotFoundException("Loan with ID " + loanId + " not found"));
    }

    private Inventory getInventoryForUpdate(Long bookId) {
        return inventoryRepository.findByBookIdForUpdate(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Inventory for book " + bookId + " not found"));
    }

    private static String describeTiming(long daysLate) {
        return switch (ReturnTiming.of(daysLate)) {
            case EARLY -> Math.abs(daysLate) + " day(s) early";
            case ON_TIME -> "on time";
            case LATE -> daysLate + " day(s) late";
        };
    }

    private void requireTransition(Loan loan, LoanStatus next) {
        if (!loan.getStatus().canChangeTo(next)) {
            throw new InvalidStateException(
                    "A loan that is " + loan.getStatus() + " cannot be changed to " + next);
        }
    }

    /**
     * Adds one copy back to the book inventory, never going above the total number of copies.
     */
    private void giveCopyBack(Long bookId) {
        Inventory inventory = getInventoryForUpdate(bookId);
        // Never exceed the stock, even if the data was edited by hand.
        inventory.setAvailableCopies(Math.min(inventory.getTotalCopies(), inventory.getAvailableCopies() + 1));
    }

    /**
     * Takes a damaged copy out of circulation. The copy was already out of the available stock,
     * so only the total goes down.
     */
    private void withdrawCopy(Long bookId) {
        Inventory inventory = getInventoryForUpdate(bookId);
        int total = Math.max(0, inventory.getTotalCopies() - 1);
        inventory.setTotalCopies(total);
        inventory.setAvailableCopies(Math.min(inventory.getAvailableCopies(), total));
    }

    private static String fineReason(boolean damaged, boolean late, long daysLate, String notes) {
        List<String> parts = new ArrayList<>();
        if (damaged) {
            parts.add("Damaged on return");
        }
        if (late) {
            parts.add("Returned " + daysLate + " day(s) late");
        }
        String reason = parts.isEmpty() ? "Fine added by librarian" : String.join(", ", parts);
        return notes == null ? reason : reason + ": " + notes;
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

    /**
     * Runs the action only after the transaction commits, so users are never notified about a change that was rolled back.
     */
    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
