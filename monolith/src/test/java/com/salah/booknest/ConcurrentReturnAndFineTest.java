package com.salah.booknest;

import com.salah.booknest.model.Book;
import com.salah.booknest.model.Fine;
import com.salah.booknest.model.FineStatus;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.ReviewReturnRequest;
import com.salah.booknest.repository.FineRepository;
import com.salah.booknest.service.FineService;
import com.salah.booknest.service.LoanService;
import com.salah.booknest.support.ConcurrentRunner;
import com.salah.booknest.support.ConcurrentRunner.Outcome;
import com.salah.booknest.support.TestData;
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
class ConcurrentReturnAndFineTest {

    @Autowired private LoanService loanService;
    @Autowired private FineService fineService;
    @Autowired private FineRepository fineRepository;
    @Autowired private TestData data;

    @Test
    void sameFinePaidFiveTimesAtOnce_onlyOnePaymentGoesThrough() throws Exception {
        User member = data.newUser();
        Loan loan = data.newLoan(member, data.newBook(1));
        Fine fine = data.newFine(loan);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> fineService.payFine(fine.getId(), data.auth(member)));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
        assertEquals(4, ConcurrentRunner.countErrorCode(outcomes, "FINE_ALREADY_PAID"));
        assertEquals(FineStatus.PAID, fineRepository.findById(fine.getId()).get().getStatus());
    }

    @Test
    void twoLibrariansReviewTheSameReturn_copyGoesBackOnlyOnce() throws Exception {
        Book book = data.newBook(1);
        User member = data.newUser();
        User librarianOne = data.newLibrarian();
        User librarianTwo = data.newLibrarian();
        Loan loan = data.newLoan(member, book);
        loanService.returnLoan(loan.getId(), data.auth(member));

        ReviewReturnRequest review = new ReviewReturnRequest(false, null, null);
        List<Callable<Object>> tasks = List.of(
                () -> loanService.reviewReturn(loan.getId(), review, data.auth(librarianOne)),
                () -> loanService.reviewReturn(loan.getId(), review, data.auth(librarianTwo)));

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
        assertEquals(1, data.inventory(book).getAvailableCopies());
        assertEquals(1, data.inventory(book).getTotalCopies());
        data.assertStockConsistent(book);
    }

    @Test
    void memberReturnsTheSameLoanTwiceAtOnce_onlyOneReturnCounts() throws Exception {
        Book book = data.newBook(1);
        User member = data.newUser();
        Loan loan = data.newLoan(member, book);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> loanService.returnLoan(loan.getId(), data.auth(member)));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(1, ConcurrentRunner.countSucceeded(outcomes));
        data.assertStockConsistent(book);
    }
}
