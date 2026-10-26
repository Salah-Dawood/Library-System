package com.salah.booknest.config;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Fine;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.Review;
import com.salah.booknest.model.User;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.FineRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.ReviewRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Adds sample loans and reviews (and their audit entries) for the users and books created by DatabaseSeeder,
 * so the whole workflow can be shown straight after startup.
 * Dates are relative to today, so "overdue" or "early" are always true when the app is started.
 */
@Component
@Order(2) // runs after DatabaseSeeder (@Order(1)) has created the users and books
@RequiredArgsConstructor
public class SampleActivitySeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final ReviewRepository reviewRepository;
    private final FineRepository fineRepository;
    private final AuditLogService auditLogService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void run(String... args) {
        if (loanRepository.count() > 0 || reviewRepository.count() > 0) {
            System.out.println("Loans or reviews already exist. Skipping sample loans and reviews.");
            return;
        }
        try {
            // One transaction for everything: if a sample user or book is missing, nothing is saved
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> seedSampleActivity());
            System.out.println("Sample loans and reviews successfully seeded!");
        } catch (InformationNotFoundException e) {
            System.out.println("Skipping sample loans and reviews: " + e.getMessage());
        }
    }

    private void seedSampleActivity() {
        User librarian = findUser("librarian");
        User john = findUser("johnd");
        User jane = findUser("janed");
        User alex = findUser("alexm");

        Book nineteen84 = findBook("1984");
        Book animalFarm = findBook("Animal Farm");
        Book harryPotter = findBook("Harry Potter and the Sorcerer's Stone");
        Book hobbit = findBook("The Hobbit");
        Book fellowship = findBook("The Fellowship of the Ring");
        Book mockingbird = findBook("To Kill a Mockingbird");
        Book allQuiet = findBook("All Quiet on the Western Front");
        Book roadBack = findBook("The Road Back");
        Book foundation = findBook("Foundation");
        Book iRobot = findBook("I, Robot");
        Book orientExpress = findBook("Murder on the Orient Express");
        Book noneLeft = findBook("And Then There Were None");
        Book dune = findBook("Dune");
        Book duneMessiah = findBook("Dune Messiah");


        // johnd: a reliable member. Every return was on time or early, which earns the "Reliable member" badge.
        returnedAndCleared(librarian, john, nineteen84, 14, 40, -2);
        returnedAndCleared(librarian, john, hobbit, 14, 30, 0);
        returnedAndCleared(librarian, john, dune, 7, 20, -1);
        borrowed(john, foundation, 14, 5);
        borrowed(john, orientExpress, 7, 1);

        // janed: late returns, one overdue loan, one late return waiting for review, one damaged return with a fine
        returnedAndCleared(librarian, jane, animalFarm, 14, 35, 3);
        returnedAndCleared(librarian, jane, mockingbird, 14, 25, 0);
        returnedDamaged(librarian, jane, roadBack, 14, 45, 1, "Water damage on the cover", new BigDecimal("5.00"));
        borrowed(jane, iRobot, 14, 20); // due 6 days ago, so it shows as overdue
        borrowed(jane, harryPotter, 14, 3);
        returnedPendingReview(jane, duneMessiah, 14, 20, 2); // returned 2 days late, waiting for the librarian

        // alexm: a newer member with one return, one active loan, one on-time return in the review queue
        // and one loan a librarian canceled
        returnedAndCleared(librarian, alex, hobbit, 14, 18, -2);
        borrowed(alex, allQuiet, 10, 2);
        returnedPendingReview(alex, fellowship, 7, 8, 0);
        cancelledByLibrarian(librarian, alex, noneLeft, 7, 3);

        // Reviews. Only members who returned a book can review it (see ReviewService), so every review
        // below matches a returned loan above. johnd returned "Dune" but has not reviewed it yet, so
        // the "write a review" step can still be shown live.
        review(john, nineteen84, 9, "Chilling and still painfully relevant. Hard to put down.");
        review(john, hobbit, 8, "A cozy adventure with great pacing.");
        review(alex, hobbit, 9, "The perfect way into Middle-earth.");
        review(jane, animalFarm, 7, "Short, sharp and surprisingly funny in places.");
        review(jane, mockingbird, 10, "A moving story with unforgettable characters.");
    }

    // ---- loan states ----

    /** A loan that is still out. It takes one copy from the stock. */
    private void borrowed(User member, Book book, int days, int loanedDaysAgo) {
        Loan loan = newLoan(member, book, LoanStatus.BORROWED, days, loanedDaysAgo);
        loan = loanRepository.save(loan);
        takeCopy(book);

        audit("LOAN", member, "BORROWED", loan.getId());
    }

    /** A returned loan a librarian checked and accepted. The copy is back in stock, so the stock is unchanged. */
    private void returnedAndCleared(User librarian, User member, Book book, int days, int loanedDaysAgo, int returnedDaysAfterDue) {
        Loan loan = newLoan(member, book, LoanStatus.RETURNED, days, loanedDaysAgo);
        loan.setReturnDate(loan.getDueDate().plusDays(returnedDaysAfterDue));
        markReviewed(loan, librarian, false, null);
        loan = loanRepository.save(loan);

        audit("LOAN", member, "BORROWED", loan.getId());
        audit("LOAN", member, "RETURNED", loan.getId());
        audit("LOAN", librarian, "RETURN_CLEARED", loan.getId());
    }

    /** A returned loan where the librarian found damage and charged a fine. The copy is withdrawn from stock. */
    private void returnedDamaged(User librarian, User member, Book book, int days, int loanedDaysAgo,
                                 int returnedDaysAfterDue, String notes, BigDecimal fineAmount) {
        Loan loan = newLoan(member, book, LoanStatus.RETURNED, days, loanedDaysAgo);
        loan.setReturnDate(loan.getDueDate().plusDays(returnedDaysAfterDue));
        markReviewed(loan, librarian, true, notes);
        loan = loanRepository.save(loan);
        withdrawCopy(book);

        Fine fine = new Fine();
        fine.setLoan(loan);
        fine.setAmount(fineAmount);
        fine.setReason("Damaged on return, returned " + returnedDaysAfterDue + " day(s) late: " + notes);
        fineRepository.save(fine);

        audit("LOAN", member, "BORROWED", loan.getId());
        audit("LOAN", member, "RETURNED", loan.getId());
        audit("LOAN", librarian, "RETURN_DAMAGED", loan.getId());
    }

    /** A returned loan waiting in the review queue. The copy has not gone back to stock yet. */
    private void returnedPendingReview(User member, Book book, int days, int loanedDaysAgo, int returnedDaysAfterDue) {
        Loan loan = newLoan(member, book, LoanStatus.PENDING_REVIEW, days, loanedDaysAgo);
        loan.setReturnDate(loan.getDueDate().plusDays(returnedDaysAfterDue));
        loan = loanRepository.save(loan);
        takeCopy(book);

        audit("LOAN", member, "BORROWED", loan.getId());
        audit("LOAN", member, "RETURNED", loan.getId());
    }

    /** A loan a librarian cancelled while it was out. The copy went back, so the stock is unchanged. */
    private void cancelledByLibrarian(User librarian, User member, Book book, int days, int loanedDaysAgo) {
        Loan loan = loanRepository.save(newLoan(member, book, LoanStatus.CANCELLED, days, loanedDaysAgo));

        audit("LOAN", member, "BORROWED", loan.getId());
        audit("LOAN", librarian, "CANCELLED", loan.getId());
    }

    // ---- reviews ----

    private void review(User member, Book book, int rating, String comment) {
        Review review = new Review();
        review.setUser(member);
        review.setBook(book);
        review.setRating(rating);
        review.setComment(comment);
        review = reviewRepository.save(review);

        audit("REVIEW", member, "CREATED", review.getId());
    }

    // ---- helpers ----

    /** Same dates as LoanService.borrow: loan date is the borrow day, due date is loan date + days. */
    private Loan newLoan(User member, Book book, LoanStatus status, int days, int loanedDaysAgo) {
        Loan loan = new Loan();
        loan.setUser(member);
        loan.setBook(book);
        loan.setStatus(status);
        loan.setRequestedDays(days);
        LocalDate loanDate = LocalDate.now().minusDays(loanedDaysAgo);
        loan.setLoanDate(loanDate);
        loan.setDueDate(loanDate.plusDays(days));
        return loan;
    }

    private void markReviewed(Loan loan, User librarian, boolean damaged, String notes) {
        loan.setDamaged(damaged);
        loan.setReviewNotes(notes);
        loan.setReviewedBy(librarian);
        loan.setReviewedAt(loan.getReturnDate().atTime(11, 0));
    }

    /** A damaged copy leaves circulation: it was already out of the available stock, so only the total drops. */
    private void withdrawCopy(Book book) {
        Inventory inventory = book.getInventory();
        int total = Math.max(0, inventory.getTotalCopies() - 1);
        inventory.setTotalCopies(total);
        inventory.setAvailableCopies(Math.min(inventory.getAvailableCopies(), total));
    }

    private void takeCopy(Book book) {
        Inventory inventory = book.getInventory();
        inventory.setAvailableCopies(Math.max(0, inventory.getAvailableCopies() - 1));
    }

    private void audit(String type, User actor, String action, Long whatId) {
        auditLogService.logAs(type, actor.getId(), action, whatId);
    }

    private User findUser(String username) {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("User '" + username + "' not found"));
    }

    private Book findBook(String title) {
        return bookRepository.findByTitle(title)
                .orElseThrow(() -> new InformationNotFoundException("Book '" + title + "' not found"));
    }
}
