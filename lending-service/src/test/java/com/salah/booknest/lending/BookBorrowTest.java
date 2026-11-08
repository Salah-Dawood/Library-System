package com.salah.booknest.lending;

import com.salah.booknest.lending.exception.InformationExistException;
import com.salah.booknest.lending.exception.InvalidRequestException;
import com.salah.booknest.lending.exception.InvalidStateException;
import com.salah.booknest.lending.model.Fine;
import com.salah.booknest.lending.model.FineType;
import com.salah.booknest.lending.model.Inventory;
import com.salah.booknest.lending.model.Loan;
import com.salah.booknest.lending.model.LoanStatus;
import com.salah.booknest.lending.model.ReturnTiming;
import com.salah.booknest.lending.model.Member;
import com.salah.booknest.lending.model.request.LoanRequest;
import com.salah.booknest.lending.model.request.ReviewReturnRequest;
import com.salah.booknest.lending.model.response.LoanResponse;
import com.salah.booknest.lending.repository.InventoryRepository;
import com.salah.booknest.lending.repository.FineRepository;
import com.salah.booknest.lending.repository.LoanRepository;
import com.salah.booknest.lending.security.TokenUser;
import com.salah.booknest.lending.service.AuditLogService;
import com.salah.booknest.lending.service.InventoryLocker;
import com.salah.booknest.lending.service.MemberService;
import com.salah.booknest.lending.service.LoanService;
import com.salah.booknest.lending.service.NotificationService;
import com.salah.booknest.lending.service.WaitlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookBorrowTest {

    @Mock private LoanRepository loanRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryLocker inventoryLocker;
    @Mock private FineRepository fineRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private MemberService memberService;
    @Mock private WaitlistService waitlistService;

    @InjectMocks
    private LoanService loanService;

    private Member member;
    private Inventory inventory;
    private Authentication memberAuth;
    private Authentication otherMemberAuth;
    private Authentication librarianAuth;

    @BeforeEach
    void setUp() {
        // LoanService registers after-commit callbacks, which need an active synchronization.
        TransactionSynchronizationManager.initSynchronization();
        // the late fine per day normally comes from application.properties
        ReflectionTestUtils.setField(loanService, "latePerDay", new BigDecimal("0.35"));

        member = new Member(1L, "alice");

        inventory = new Inventory();
        inventory.setBookId(10L);
        inventory.setBookTitle("Clean Code");
        inventory.setActive(true);
        inventory.setTotalCopies(3);
        inventory.setAvailableCopies(3);

        memberAuth = auth(1L, "alice", false);
        otherMemberAuth = auth(2L, "bob", false);
        librarianAuth = auth(3L, "lib", true);

        when(memberService.lockCurrentMember(memberAuth)).thenReturn(member);
        when(inventoryRepository.findByBookId(10L)).thenReturn(Optional.of(inventory));
        when(inventoryLocker.lock(10L)).thenReturn(inventory);
        // nobody is waiting, so a returned copy goes back on the shelf
        doAnswer(call -> {
            Inventory i = call.getArgument(0);
            i.setAvailableCopies(Math.min(i.getTotalCopies(), i.getAvailableCopies() + 1));
            return null;
        }).when(waitlistService).offerCopy(any(Inventory.class));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setId(100L);
            return l;
        });
        when(loanRepository.existsByUserIdAndBookIdAndStatusIn(anyLong(), anyLong(), anyCollection()))
                .thenReturn(false);
        when(loanRepository.countByUserIdAndStatusIn(anyLong(), anyCollection())).thenReturn(0L);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // borrow

    @Test
    void borrow_validRequest_createsBorrowedLoanAndTakesOneCopy() {
        LoanResponse response = loanService.borrow(memberAuth, request(10L, 14));

        assertEquals(LoanStatus.BORROWED, response.status());
        assertEquals("alice", response.username());
        assertEquals(Integer.valueOf(14), response.requestedDays());
        assertEquals(LocalDate.now(), response.loanDate());
        assertEquals(LocalDate.now().plusDays(14), response.dueDate());
        assertEquals(Integer.valueOf(2), inventory.getAvailableCopies());
        verify(auditLogService).log("LOAN", "BORROWED", 100L);
    }

    @Test
    void borrow_durationOverThirtyDays_throws() {
        assertThrows(InvalidRequestException.class, () -> loanService.borrow(memberAuth, request(10L, 31)));
    }

    @Test
    void borrow_missingBookId_throws() {
        assertThrows(InvalidRequestException.class, () -> loanService.borrow(memberAuth, request(null, 7)));
    }

    @Test
    void borrow_sameBookAlreadyOnLoan_throws() {
        when(loanRepository.existsByUserIdAndBookIdAndStatusIn(eq(1L), eq(10L), anyCollection()))
                .thenReturn(true);

        assertThrows(InformationExistException.class, () -> loanService.borrow(memberAuth, request(10L, 7)));
    }

    @Test
    void borrow_fiveActiveLoans_throws() {
        when(loanRepository.countByUserIdAndStatusIn(eq(1L), anyCollection())).thenReturn(5L);

        InvalidStateException e = assertThrows(InvalidStateException.class,
                () -> loanService.borrow(memberAuth, request(10L, 7)));
        assertEquals("LOAN_LIMIT_REACHED", e.getCode());
    }

    @Test
    void borrow_noCopiesAvailable_throwsAndSavesNothing() {
        inventory.setAvailableCopies(0);

        InvalidStateException e = assertThrows(InvalidStateException.class,
                () -> loanService.borrow(memberAuth, request(10L, 7)));
        assertEquals("NO_COPIES_AVAILABLE", e.getCode());
        assertEquals(Integer.valueOf(0), inventory.getAvailableCopies());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void borrow_locksTheMemberBeforeTheBook() {
        loanService.borrow(memberAuth, request(10L, 7));

        InOrder order = inOrder(memberService, inventoryLocker);
        order.verify(memberService).lockCurrentMember(memberAuth);
        order.verify(inventoryLocker).lock(10L);
    }

    @Test
    void borrow_inactiveBook_throws() {
        inventory.setActive(false);

        assertThrows(InvalidStateException.class, () -> loanService.borrow(memberAuth, request(10L, 7)));
    }

    // return (member hands the book back, librarian reviews it later)

    @Test
    void returnLoan_lateReturn_waitsForReviewAndChargesLateFine() {
        inventory.setAvailableCopies(2);
        Loan loan = loan(LoanStatus.BORROWED, 7);
        loan.setDueDate(LocalDate.now().minusDays(3));
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan));

        LoanResponse response = loanService.returnLoan(100L, memberAuth);

        assertEquals(LoanStatus.PENDING_REVIEW, response.status());
        assertEquals(ReturnTiming.LATE, response.returnTiming());
        assertEquals(Long.valueOf(3), response.daysLate());
        assertEquals(Integer.valueOf(2), inventory.getAvailableCopies()); // copy is not back in stock before the review

        ArgumentCaptor<Fine> fine = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository).save(fine.capture());
        assertEquals(FineType.LATE, fine.getValue().getType());
        assertEquals(0, new BigDecimal("1.05").compareTo(fine.getValue().getAmount())); // 3 days x 0.35
    }

    @Test
    void returnLoan_beforeDueDate_waitsForReviewWithoutFine() {
        Loan loan = loan(LoanStatus.BORROWED, 7);
        loan.setDueDate(LocalDate.now().plusDays(2));
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan));

        LoanResponse response = loanService.returnLoan(100L, memberAuth);

        assertEquals(LoanStatus.PENDING_REVIEW, response.status());
        assertEquals(ReturnTiming.EARLY, response.returnTiming());
        verify(fineRepository, never()).save(any(Fine.class));
    }

    @Test
    void returnLoan_byAnotherMember_throwsAccessDenied() {
        Loan loan = loan(LoanStatus.BORROWED, 7);
        loan.setDueDate(LocalDate.now().plusDays(1));
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan));

        assertThrows(AccessDeniedException.class, () -> loanService.returnLoan(100L, otherMemberAuth));
    }

    @Test
    void returnLoan_loanNotBorrowed_throws() {
        Loan loan = loan(LoanStatus.PENDING_REVIEW, 7);
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan));

        assertThrows(InvalidStateException.class, () -> loanService.returnLoan(100L, memberAuth));
    }

    // librarian review

    @Test
    void reviewReturn_notDamaged_putsCopyBackInStock() {
        inventory.setAvailableCopies(2);
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(pendingReviewLoan()));

        LoanResponse response = loanService.reviewReturn(100L, new ReviewReturnRequest(false, null, null), librarianAuth);

        assertEquals(LoanStatus.RETURNED, response.status());
        assertEquals("lib", response.reviewedBy());
        assertEquals(Boolean.FALSE, response.damaged());
        assertEquals(Integer.valueOf(3), inventory.getAvailableCopies());
        assertEquals(Integer.valueOf(3), inventory.getTotalCopies());
    }

    @Test
    void reviewReturn_damaged_removesTheCopyFromCirculation() {
        inventory.setAvailableCopies(2);
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(pendingReviewLoan()));

        LoanResponse response = loanService.reviewReturn(100L, new ReviewReturnRequest(true, "torn cover", null), librarianAuth);

        assertEquals(LoanStatus.RETURNED, response.status());
        assertEquals(Boolean.TRUE, response.damaged());
        assertEquals(Integer.valueOf(2), inventory.getTotalCopies());
        assertEquals(Integer.valueOf(2), inventory.getAvailableCopies());
    }

    @Test
    void reviewReturn_loanNotWaitingForReview_throws() {
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan(LoanStatus.BORROWED, 7)));

        assertThrows(InvalidStateException.class,
                () -> loanService.reviewReturn(100L, new ReviewReturnRequest(false, null, null), librarianAuth));
    }

    // cancel

    @Test
    void cancel_borrowedLoan_putsCopyBackInStock() {
        inventory.setAvailableCopies(2);
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan(LoanStatus.BORROWED, 7)));

        LoanResponse response = loanService.cancel(100L);

        assertEquals(LoanStatus.CANCELLED, response.status());
        assertEquals(Integer.valueOf(3), inventory.getAvailableCopies());
    }

    @Test
    void cancel_alreadyReturnedLoan_throws() {
        when(loanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(loan(LoanStatus.RETURNED, 7)));

        assertThrows(InvalidStateException.class, () -> loanService.cancel(100L));
    }

    // helpers

    private Authentication auth(Long id, String username, boolean isLibrarian) {
        String role = isLibrarian ? "ROLE_librarian" : "ROLE_member";
        return new UsernamePasswordAuthenticationToken(
                new TokenUser(id, username), null, List.of(new SimpleGrantedAuthority(role)));
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
        loan.setUserId(member.getId());
        loan.setUsername(member.getUsername());
        loan.setBookId(inventory.getBookId());
        loan.setBookTitle(inventory.getBookTitle());
        loan.setStatus(status);
        loan.setRequestedDays(requestedDays);
        loan.setLoanDate(LocalDate.now().minusDays(requestedDays));
        loan.setDueDate(LocalDate.now());
        return loan;
    }

    private Loan pendingReviewLoan() {
        Loan loan = loan(LoanStatus.PENDING_REVIEW, 7);
        loan.setReturnDate(LocalDate.now());
        return loan;
    }
}
