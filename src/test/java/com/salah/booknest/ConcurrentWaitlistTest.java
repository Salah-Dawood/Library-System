package com.salah.booknest;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.User;
import com.salah.booknest.model.WaitlistEntry;
import com.salah.booknest.model.WaitlistStatus;
import com.salah.booknest.model.request.LoanRequest;
import com.salah.booknest.model.request.ReviewReturnRequest;
import com.salah.booknest.model.request.WaitlistRequest;
import com.salah.booknest.model.response.WaitlistResponse;
import com.salah.booknest.service.LoanService;
import com.salah.booknest.service.WaitlistService;
import com.salah.booknest.support.ConcurrentRunner;
import com.salah.booknest.support.ConcurrentRunner.Outcome;
import com.salah.booknest.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentWaitlistTest {

    @Autowired private WaitlistService waitlistService;
    @Autowired private LoanService loanService;
    @Autowired private TestData data;

    @Test
    void eightMembersJoinAtOnce_everyoneGetsTheirOwnPosition() throws Exception {
        Book book = data.newBook(1);
        data.newLoan(data.newUser(), book);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            User member = data.newUser();
            tasks.add(() -> waitlistService.join(data.auth(member), new WaitlistRequest(book.getId())));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(8, ConcurrentRunner.countSucceeded(outcomes));
        Set<Integer> positions = new HashSet<>();
        for (Outcome outcome : outcomes) {
            positions.add(((WaitlistResponse) outcome.value()).position());
        }
        assertEquals(8, positions.size());
        assertTrue(positions.contains(1) && positions.contains(8));
    }

    @Test
    void sameMemberJoinsFiveTimesAtOnce_onlyOneEntryIsCreated() throws Exception {
        Book book = data.newBook(1);
        data.newLoan(data.newUser(), book);
        User member = data.newUser();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> waitlistService.join(data.auth(member), new WaitlistRequest(book.getId())));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        long duplicates = outcomes.stream().filter(o -> o.error() instanceof InformationExistException).count();
        assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
        assertEquals(4, duplicates);
    }

    @Test
    void returnedCopyIsHeldForTheFirstInLine_walkInsCannotTakeIt() throws Exception {
        Book book = data.newBook(1);
        User borrower = data.newUser();
        User librarian = data.newLibrarian();
        User firstInLine = data.newUser();
        Loan loan = data.newLoan(borrower, book);
        WaitlistEntry entry = data.newWaitingEntry(firstInLine, book);

        loanService.returnLoan(loan.getId(), data.auth(borrower));
        loanService.reviewReturn(loan.getId(), new ReviewReturnRequest(false, null, null), data.auth(librarian));
        assertEquals(WaitlistStatus.HOLD_READY, data.reload(entry).getStatus());
        assertEquals(0, data.inventory(book).getAvailableCopies());

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User walkIn = data.newUser();
            tasks.add(() -> loanService.borrow(data.auth(walkIn), new LoanRequest(book.getId(), 7)));
        }
        tasks.add(() -> loanService.claimHold(entry.getId(), 7, data.auth(firstInLine)));

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
        assertTrue(outcomes.get(5).succeeded(), "the member in line should get the copy");
        assertEquals(5, ConcurrentRunner.countErrorCode(outcomes, "NO_COPIES_AVAILABLE"));
        assertEquals(WaitlistStatus.FULFILLED, data.reload(entry).getStatus());
        data.assertStockConsistent(book);
    }

    @Test
    void sameHoldClaimedFiveTimesAtOnce_onlyOneLoanIsCreated() throws Exception {
        Book book = data.newBook(1);
        User member = data.newUser();
        WaitlistEntry entry = data.newHoldEntry(member, book, 60);
        data.setAvailable(book, 0);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> loanService.claimHold(entry.getId(), 7, data.auth(member)));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
        assertEquals(4, ConcurrentRunner.countErrorCode(outcomes, "NO_ACTIVE_HOLD"));
        data.assertStockConsistent(book);
    }

    @Test
    void sameHoldExpiredFiveTimesAtOnce_copyIsOfferedOnlyOnce() throws Exception {
        Book book = data.newBook(1);
        User member = data.newUser();
        WaitlistEntry entry = data.newHoldEntry(member, book, -5);
        data.setAvailable(book, 0);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> waitlistService.expireHold(entry.getId()));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        long expiredNow = outcomes.stream().filter(o -> Boolean.TRUE.equals(o.value())).count();
        assertTrue(expiredNow <= 1, "only one thread may expire the hold");
        assertEquals(WaitlistStatus.EXPIRED, data.reload(entry).getStatus());
        assertEquals(1, data.inventory(book).getAvailableCopies());
        data.assertStockConsistent(book);
    }

    @Test
    void holdLeftByMemberAndExpiredAtOnce_copyIsOfferedOnlyOnce() throws Exception {
        Book book = data.newBook(1);
        User member = data.newUser();
        WaitlistEntry entry = data.newHoldEntry(member, book, -5);
        data.setAvailable(book, 0);

        List<Callable<Object>> tasks = List.of(
                () -> waitlistService.leave(entry.getId(), data.auth(member)),
                () -> waitlistService.expireHold(entry.getId()));

        ConcurrentRunner.runTogether(tasks);

        assertEquals(1, data.inventory(book).getAvailableCopies());
        data.assertStockConsistent(book);
    }
}
