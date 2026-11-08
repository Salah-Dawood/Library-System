package com.salah.booknest;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.LoanRequest;
import com.salah.booknest.service.LoanService;
import com.salah.booknest.support.ConcurrentRunner;
import com.salah.booknest.support.ConcurrentRunner.Outcome;
import com.salah.booknest.support.TestData;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrentBorrowTest {

    @Autowired private LoanService loanService;
    @Autowired private TestData data;

    @RepeatedTest(5)
    void lastCopy_tenMembersBorrowTogether_onlyOneGetsIt() throws Exception {
        Book book = data.newBook(1);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User member = data.newUser();
            tasks.add(() -> loanService.borrow(data.auth(member), new LoanRequest(book.getId(), 7)));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
        assertEquals(9, ConcurrentRunner.countErrorCode(outcomes, "NO_COPIES_AVAILABLE"));
        assertEquals(0, data.inventory(book).getAvailableCopies());
        assertEquals(1, data.countLoans(book, LoanStatus.BORROWED));
        data.assertStockConsistent(book);
    }

    @Test
    void threeCopies_tenMembersBorrowTogether_exactlyThreeGetOne() throws Exception {
        Book book = data.newBook(3);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User member = data.newUser();
            tasks.add(() -> loanService.borrow(data.auth(member), new LoanRequest(book.getId(), 7)));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(3, ConcurrentRunner.countSucceeded(outcomes));
        assertEquals(7, ConcurrentRunner.countErrorCode(outcomes, "NO_COPIES_AVAILABLE"));
        assertEquals(0, data.inventory(book).getAvailableCopies());
        data.assertStockConsistent(book);
    }

    @Test
    void sameMemberBorrowsTheSameBookTwiceAtOnce_onlyOneLoanIsCreated() throws Exception {
        Book book = data.newBook(5);
        User member = data.newUser();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> loanService.borrow(data.auth(member), new LoanRequest(book.getId(), 7)));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        long duplicates = outcomes.stream().filter(o -> o.error() instanceof InformationExistException).count();
        assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
        assertEquals(4, duplicates);
        assertEquals(4, data.inventory(book).getAvailableCopies());
        data.assertStockConsistent(book);
    }

    @Test
    void memberWithFourLoansBorrowsFourMoreAtOnce_loanLimitHolds() throws Exception {
        User member = data.newUser();
        for (int i = 0; i < 4; i++) {
            data.newLoan(member, data.newBook(2));
        }
        List<Book> books = new ArrayList<>();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Book book = data.newBook(2);
            books.add(book);
            tasks.add(() -> loanService.borrow(data.auth(member), new LoanRequest(book.getId(), 7)));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
        assertEquals(3, ConcurrentRunner.countErrorCode(outcomes, "LOAN_LIMIT_REACHED"));
        for (Book book : books) {
            data.assertStockConsistent(book);
        }
    }

    @Test
    void returnAndCancelAtOnce_onlyOneOfThemWins() throws Exception {
        Book book = data.newBook(1);
        User member = data.newUser();
        Loan loan = data.newLoan(member, book);

        List<Callable<Object>> tasks = List.of(
                () -> loanService.returnLoan(loan.getId(), data.auth(member)),
                () -> loanService.cancel(loan.getId()));

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
        data.assertStockConsistent(book);
    }
}
