package com.salah.booknest.config;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.Review;
import com.salah.booknest.model.User;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.ReviewRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

        // returned(librarian, member, book, days, loanedDaysAgo, returnedDaysAfterDue)
        //   returnedDaysAfterDue: negative = early, 0 = on time, positive = late
        // approved(librarian, member, book, days, loanedDaysAgo)

        // johnd: a reliable member. Every return was on time or early, which earns the "Reliable member" badge.
        returned(librarian, john, nineteen84, 14, 40, -2);
        returned(librarian, john, hobbit, 14, 30, 0);
        returned(librarian, john, dune, 7, 20, -1);
        approved(librarian, john, foundation, 14, 5);
        requested(john, orientExpress, 7);

        // janed: one late return, one overdue loan, two pending requests and one rejected request
        returned(librarian, jane, animalFarm, 14, 35, 3);
        returned(librarian, jane, mockingbird, 14, 25, 0);
        approved(librarian, jane, iRobot, 14, 20); // due 6 days ago, so it shows as overdue
        requested(jane, duneMessiah, 7);
        requested(jane, harryPotter, 14);
        rejected(librarian, jane, roadBack, 14, "This title is reserved for a class this month");

        // alexm: a newer member with one return, one active loan, one pending and one cancelled request
        returned(librarian, alex, hobbit, 14, 18, -2);
        approved(librarian, alex, allQuiet, 10, 2);
        requested(alex, fellowship, 21);
        cancelled(alex, noneLeft, 7);

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

    /** A request that is waiting for a librarian. */
    private void requested(User member, Book book, int days) {
        Loan loan = loanRepository.save(newLoan(member, book, LoanStatus.REQUESTED, days));
        audit("LOAN", member, "REQUESTED", loan.getId());
    }

    /** A loan that was approved and is still out. It takes one copy from the stock. */
    private void approved(User librarian, User member, Book book, int days, int loanedDaysAgo) {
        Loan loan = newLoan(member, book, LoanStatus.APPROVED, days);
        setApprovalDetails(loan, librarian, loanedDaysAgo);
        loan = loanRepository.save(loan);
        takeCopy(book);

        audit("LOAN", member, "REQUESTED", loan.getId());
        audit("LOAN", librarian, "APPROVED", loan.getId());
    }

    /** A loan that was approved and then returned. The copy is back in stock, so the stock is unchanged. */
    private void returned(User librarian, User member, Book book, int days, int loanedDaysAgo, int returnedDaysAfterDue) {
        Loan loan = newLoan(member, book, LoanStatus.RETURNED, days);
        setApprovalDetails(loan, librarian, loanedDaysAgo);
        loan.setReturnDate(loan.getDueDate().plusDays(returnedDaysAfterDue));
        loan = loanRepository.save(loan);

        audit("LOAN", member, "REQUESTED", loan.getId());
        audit("LOAN", librarian, "APPROVED", loan.getId());
        audit("LOAN", member, "RETURNED", loan.getId());
    }

    /** A request a librarian turned down, with the reason shown to the member. */
    private void rejected(User librarian, User member, Book book, int days, String reason) {
        Loan loan = newLoan(member, book, LoanStatus.REJECTED, days);
        loan.setRejectionReason(reason);
        loan.setDecidedBy(librarian);
        loan.setDecidedAt(LocalDateTime.now().minusDays(3));
        loan = loanRepository.save(loan);

        audit("LOAN", member, "REQUESTED", loan.getId());
        audit("LOAN", librarian, "REJECTED", loan.getId());
    }

    /** A request the member cancelled themselves (no librarian decision is recorded for that). */
    private void cancelled(User member, Book book, int days) {
        Loan loan = loanRepository.save(newLoan(member, book, LoanStatus.CANCELLED, days));

        audit("LOAN", member, "REQUESTED", loan.getId());
        audit("LOAN", member, "CANCELLED", loan.getId());
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

    private Loan newLoan(User member, Book book, LoanStatus status, int days) {
        Loan loan = new Loan();
        loan.setUser(member);
        loan.setBook(book);
        loan.setStatus(status);
        loan.setRequestedDays(days);
        return loan;
    }

    /** Same as LoanService.approve: loan date, due date (loan date + requested days) and who decided. */
    private void setApprovalDetails(Loan loan, User librarian, int loanedDaysAgo) {
        LocalDate loanDate = LocalDate.now().minusDays(loanedDaysAgo);
        loan.setLoanDate(loanDate);
        loan.setDueDate(loanDate.plusDays(loan.getRequestedDays()));
        loan.setDecidedBy(librarian);
        loan.setDecidedAt(loanDate.atTime(10, 0));
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
