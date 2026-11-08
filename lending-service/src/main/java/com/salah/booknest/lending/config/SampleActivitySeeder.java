package com.salah.booknest.lending.config;

import com.salah.booknest.lending.exception.InformationNotFoundException;
import com.salah.booknest.lending.model.Fine;
import com.salah.booknest.lending.model.Inventory;
import com.salah.booknest.lending.model.Loan;
import com.salah.booknest.lending.model.LoanStatus;
import com.salah.booknest.lending.model.Member;
import com.salah.booknest.lending.model.WaitlistEntry;
import com.salah.booknest.lending.model.WaitlistStatus;
import com.salah.booknest.lending.repository.InventoryRepository;
import com.salah.booknest.lending.repository.FineRepository;
import com.salah.booknest.lending.repository.LoanRepository;
import com.salah.booknest.lending.repository.MemberRepository;
import com.salah.booknest.lending.repository.WaitlistRepository;
import com.salah.booknest.lending.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Adds sample loans, fines and waitlist entries (and their audit entries) for the members and stock created by DatabaseSeeder,
 * so the whole workflow can be shown straight after startup.
 * Dates are relative to today, so "overdue" or "early" are always true when the app is started.
 */
@Component
@Profile("!test")
@Order(2) // runs after DatabaseSeeder (@Order(1)) has created the members and stock
@Slf4j
@RequiredArgsConstructor
public class SampleActivitySeeder implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final InventoryRepository inventoryRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final AuditLogService auditLogService;
    private final PlatformTransactionManager transactionManager;
    private final WaitlistRepository waitlistRepository;

    @Value("${app.holds.hours}")
    private int holdHours;

    @Override
    public void run(String... args) {
        if (loanRepository.count() > 0) {
            log.info("Loans already exist. Skipping sample activity.");
            return;
        }
        try {
            // One transaction for everything: if a sample member or book is missing, nothing is saved
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> seedSampleActivity());
            log.info("Sample loans, fines and waitlist entries successfully seeded!");
        } catch (InformationNotFoundException e) {
            log.warn("Skipping sample activity: {}", e.getMessage());
        }
    }

    private void seedSampleActivity() {
        Member librarian = findMember("librarian");
        Member john = findMember("johnd");
        Member jane = findMember("janed");
        Member alex = findMember("alexm");

        Inventory nineteen84 = findBook("1984");
        Inventory animalFarm = findBook("Animal Farm");
        Inventory harryPotter = findBook("Harry Potter and the Sorcerer's Stone");
        Inventory hobbit = findBook("The Hobbit");
        Inventory fellowship = findBook("The Fellowship of the Ring");
        Inventory mockingbird = findBook("To Kill a Mockingbird");
        Inventory allQuiet = findBook("All Quiet on the Western Front");
        Inventory roadBack = findBook("The Road Back");
        Inventory foundation = findBook("Foundation");
        Inventory iRobot = findBook("I, Robot");
        Inventory orientExpress = findBook("Murder on the Orient Express");
        Inventory noneLeft = findBook("And Then There Were None");
        Inventory dune = findBook("Dune");
        Inventory duneMessiah = findBook("Dune Messiah");
        Inventory twoTowers = findBook("The Two Towers");
        Inventory orderOfPhoenix = findBook("Harry Potter and the Order of the Phoenix");


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

        // Waitlist: "The Two Towers" has one copy and johnd has it, so janed and alexm are waiting in line
        setStock(twoTowers, 1);
        borrowed(john, twoTowers, 14, 4);
        waiting(jane, twoTowers);
        waiting(alex, twoTowers);

        // Active hold: johnd returned the only copy of "Order of the Phoenix" yesterday, it is now held
        // for alexm (first in line) and janed waits second
        setStock(orderOfPhoenix, 1);
        returnedAndCleared(librarian, john, orderOfPhoenix, 7, 8, 0);
        holdReady(alex, orderOfPhoenix);
        waiting(jane, orderOfPhoenix);
    }

    // ---- waitlist ----

    private void waiting(Member member, Inventory book) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setUserId(member.getId());
        entry.setUsername(member.getUsername());
        entry.setBookId(book.getBookId());
        entry.setBookTitle(book.getBookTitle());
        entry = waitlistRepository.save(entry);

        audit("WAITLIST", member, "JOINED", entry.getId());
    }

    /** First in line when a copy came back. The copy is held for the member, so it is not available. */
    private void holdReady(Member member, Inventory book) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setUserId(member.getId());
        entry.setUsername(member.getUsername());
        entry.setBookId(book.getBookId());
        entry.setBookTitle(book.getBookTitle());
        entry.setStatus(WaitlistStatus.HOLD_READY);
        entry.setHoldExpiresAt(LocalDateTime.now().plusHours(holdHours));
        entry = waitlistRepository.save(entry);
        takeCopy(book);

        audit("WAITLIST", member, "JOINED", entry.getId());
        audit("WAITLIST", member, "HOLD_STARTED", entry.getId());
    }

    // ---- loan states ----

    /** A loan that is still out. It takes one copy from the stock. */
    private void borrowed(Member member, Inventory book, int days, int loanedDaysAgo) {
        Loan loan = newLoan(member, book, LoanStatus.BORROWED, days, loanedDaysAgo);
        loan = loanRepository.save(loan);
        takeCopy(book);

        audit("LOAN", member, "BORROWED", loan.getId());
    }

    /** A returned loan a librarian checked and accepted. The copy is back in stock, so the stock is unchanged. */
    private void returnedAndCleared(Member librarian, Member member, Inventory book, int days, int loanedDaysAgo, int returnedDaysAfterDue) {
        Loan loan = newLoan(member, book, LoanStatus.RETURNED, days, loanedDaysAgo);
        loan.setReturnDate(loan.getDueDate().plusDays(returnedDaysAfterDue));
        markReviewed(loan, librarian, false, null);
        loan = loanRepository.save(loan);

        audit("LOAN", member, "BORROWED", loan.getId());
        audit("LOAN", member, "RETURNED", loan.getId());
        audit("LOAN", librarian, "RETURN_CLEARED", loan.getId());
    }

    /** A returned loan where the librarian found damage and charged a fine. The copy is withdrawn from stock. */
    private void returnedDamaged(Member librarian, Member member, Inventory book, int days, int loanedDaysAgo,
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
    private void returnedPendingReview(Member member, Inventory book, int days, int loanedDaysAgo, int returnedDaysAfterDue) {
        Loan loan = newLoan(member, book, LoanStatus.PENDING_REVIEW, days, loanedDaysAgo);
        loan.setReturnDate(loan.getDueDate().plusDays(returnedDaysAfterDue));
        loan = loanRepository.save(loan);
        takeCopy(book);

        audit("LOAN", member, "BORROWED", loan.getId());
        audit("LOAN", member, "RETURNED", loan.getId());
    }

    /** A loan a librarian cancelled while it was out. The copy went back, so the stock is unchanged. */
    private void cancelledByLibrarian(Member librarian, Member member, Inventory book, int days, int loanedDaysAgo) {
        Loan loan = loanRepository.save(newLoan(member, book, LoanStatus.CANCELLED, days, loanedDaysAgo));

        audit("LOAN", member, "BORROWED", loan.getId());
        audit("LOAN", librarian, "CANCELLED", loan.getId());
    }

    // ---- helpers ----

    /** Same dates as LoanService.borrow: loan date is the borrow day, due date is loan date + days. */
    private Loan newLoan(Member member, Inventory book, LoanStatus status, int days, int loanedDaysAgo) {
        Loan loan = new Loan();
        loan.setUserId(member.getId());
        loan.setUsername(member.getUsername());
        loan.setBookId(book.getBookId());
        loan.setBookTitle(book.getBookTitle());
        loan.setStatus(status);
        loan.setRequestedDays(days);
        LocalDate loanDate = LocalDate.now().minusDays(loanedDaysAgo);
        loan.setLoanDate(loanDate);
        loan.setDueDate(loanDate.plusDays(days));
        return loan;
    }

    private void markReviewed(Loan loan, Member librarian, boolean damaged, String notes) {
        loan.setDamaged(damaged);
        loan.setReviewNotes(notes);
        loan.setReviewedBy(librarian.getUsername());
        loan.setReviewedAt(loan.getReturnDate().atTime(11, 0));
    }

    /** A damaged copy leaves circulation: it was already out of the available stock, so only the total drops. */
    private void withdrawCopy(Inventory inventory) {
        int total = Math.max(0, inventory.getTotalCopies() - 1);
        inventory.setTotalCopies(total);
        inventory.setAvailableCopies(Math.min(inventory.getAvailableCopies(), total));
    }

    private void setStock(Inventory inventory, int copies) {
        inventory.setTotalCopies(copies);
        inventory.setAvailableCopies(copies);
    }

    private void takeCopy(Inventory inventory) {
        inventory.setAvailableCopies(Math.max(0, inventory.getAvailableCopies() - 1));
    }

    private void audit(String type, Member actor, String action, Long whatId) {
        auditLogService.logAs(type, actor.getId(), action, whatId);
    }

    private Member findMember(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Member '" + username + "' not found"));
    }

    private Inventory findBook(String title) {
        return inventoryRepository.findByBookTitle(title)
                .orElseThrow(() -> new InformationNotFoundException("Book '" + title + "' not found"));
    }
}