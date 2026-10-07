package com.salah.booknest;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.ReturnTiming;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.LoanRequest;
import com.salah.booknest.model.response.LoanResponse;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.InventoryRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.service.AuditLogService;
import com.salah.booknest.service.LoanService;
import com.salah.booknest.service.NotificationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BookBorrowTest {

    @Mock private LoanRepository loanRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookRepository bookRepository;
    @Mock private InventoryRepository inventoryRepository;

    @InjectMocks
    private LoanService loanService;

    private User member;
    private User librarian;
    private Book book;
    private Inventory inventory;
    private Authentication memberAuth;
    private Authentication otherMemberAuth;
    private Authentication librarianAuth;

    @Before
    public void setUp() {
        // LoanService registers after-commit callbacks, which need an active synchronization.
        TransactionSynchronizationManager.initSynchronization();

        member = user(1L, "alice");
        librarian = user(3L, "lib");
        User otherMember = user(2L, "bob");

        book = new Book();
        book.setId(10L);
        book.setTitle("Clean Code");

        inventory = new Inventory();
        inventory.setBook(book);
        inventory.setTotalCopies(3);
        inventory.setAvailableCopies(3);

        memberAuth = auth("alice", false);
        otherMemberAuth = auth("bob", false);
        librarianAuth = auth("lib", true);

        when(userRepository.findUserByUsername("alice")).thenReturn(Optional.of(member));
        when(userRepository.findUserByUsername("bob")).thenReturn(Optional.of(otherMember));
        when(userRepository.findUserByUsername("lib")).thenReturn(Optional.of(librarian));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(inventoryRepository.findByBookIdForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setId(100L);
            return l;
        });
        when(loanRepository.existsByUserIdAndBookIdAndStatusIn(anyLong(), anyLong(), anyCollection()))
                .thenReturn(false);
        when(loanRepository.countByUserIdAndStatusIn(anyLong(), anyCollection())).thenReturn(0L);
    }

    @After
    public void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    public void requestLoan_validRequest_createsRequestedLoan() {
        LoanResponse response = loanService.requestLoan(memberAuth, request(10L, 14));

        assertEquals(LoanStatus.REQUESTED, response.status());
        assertEquals("alice", response.username());
        assertEquals(Integer.valueOf(14), response.requestedDays());
        assertEquals(Integer.valueOf(3), inventory.getAvailableCopies()); // stock untouched until approval
    }

    @Test(expected = InvalidRequestException.class)
    public void requestLoan_durationOverThirtyDays_throws() {
        loanService.requestLoan(memberAuth, request(10L, 31));
    }

    @Test(expected = InformationExistException.class)
    public void requestLoan_sameBookAlreadyActive_throws() {
        when(loanRepository.existsByUserIdAndBookIdAndStatusIn(eq(1L), eq(10L), anyCollection()))
                .thenReturn(true);

        loanService.requestLoan(memberAuth, request(10L, 7));
    }

    @Test(expected = InvalidStateException.class)
    public void requestLoan_fiveActiveLoans_throws() {
        when(loanRepository.countByUserIdAndStatusIn(eq(1L), anyCollection())).thenReturn(5L);

        loanService.requestLoan(memberAuth, request(10L, 7));
    }

    @Test
    public void approve_requestedLoan_decrementsStockAndSetsDueDate() {
        Loan loan = loan(LoanStatus.REQUESTED, 14);
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan));

        LoanResponse response = loanService.approve(100L, librarianAuth);

        assertEquals(LoanStatus.APPROVED, response.status());
        assertEquals(Integer.valueOf(2), inventory.getAvailableCopies());
        assertEquals(LocalDate.now().plusDays(14), loan.getDueDate());
        assertEquals("lib", response.decidedBy());
    }

    @Test(expected = InvalidStateException.class)
    public void approve_noCopiesAvailable_throws() {
        inventory.setAvailableCopies(0);
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan(LoanStatus.REQUESTED, 7)));

        loanService.approve(100L, librarianAuth);
    }

    @Test
    public void returnLoan_lateReturn_restoresStockAndMarksLate() {
        inventory.setAvailableCopies(2);
        Loan loan = loan(LoanStatus.APPROVED, 7);
        loan.setDueDate(LocalDate.now().minusDays(3));
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan));

        LoanResponse response = loanService.returnLoan(100L, memberAuth);

        assertEquals(LoanStatus.RETURNED, response.status());
        assertEquals(Integer.valueOf(3), inventory.getAvailableCopies());
        assertEquals(ReturnTiming.LATE, response.returnTiming());
        assertEquals(Long.valueOf(3), response.daysLate());
    }

    @Test(expected = AccessDeniedException.class)
    public void returnLoan_byAnotherMember_throwsAccessDenied() {
        Loan loan = loan(LoanStatus.APPROVED, 7);
        loan.setDueDate(LocalDate.now().plusDays(1));
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan));

        loanService.returnLoan(100L, otherMemberAuth);
    }

    // ------------------------------------------------------------ helpers

    private User user(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private Authentication auth(String username, boolean isLibrarian) {
        String role = isLibrarian ? "ROLE_librarian" : "ROLE_member";
        return new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(role)));
    }

    /** LoanRequest only has getters, so it is mocked. */
    private LoanRequest request(Long bookId, Integer duration) {
        LoanRequest request = mock(LoanRequest.class);
        when(request.getBookId()).thenReturn(bookId);
        when(request.getDuration()).thenReturn(duration);
        return request;
    }

    private Loan loan(LoanStatus status, int requestedDays) {
        Loan loan = new Loan();
        loan.setId(100L);
        loan.setUser(member);
        loan.setBook(book);
        loan.setStatus(status);
        loan.setRequestedDays(requestedDays);
        return loan;
    }
}
