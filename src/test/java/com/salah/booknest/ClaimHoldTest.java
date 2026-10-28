package com.salah.booknest;

import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.User;
import com.salah.booknest.model.WaitlistEntry;
import com.salah.booknest.model.WaitlistStatus;
import com.salah.booknest.model.response.LoanResponse;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.FineRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.service.AppSharedService;
import com.salah.booknest.service.AsyncEmailService;
import com.salah.booknest.service.AuditLogService;
import com.salah.booknest.service.InventoryLocker;
import com.salah.booknest.service.LoanService;
import com.salah.booknest.service.NotificationService;
import com.salah.booknest.service.WaitlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimHoldTest {

    @Mock private LoanRepository loanRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookRepository bookRepository;
    @Mock private InventoryLocker inventoryLocker;
    @Mock private FineRepository fineRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private AppSharedService appSharedService;
    @Mock private WaitlistService waitlistService;
    @Mock private AsyncEmailService asyncEmailService;

    @InjectMocks
    private LoanService loanService;

    private User member;
    private Book book;
    private WaitlistEntry hold;
    private Authentication memberAuth;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();

        member = new User();
        member.setId(1L);
        member.setUsername("alice");

        book = new Book();
        book.setId(10L);
        book.setTitle("Clean Code");

        hold = new WaitlistEntry();
        hold.setId(50L);
        hold.setUser(member);
        hold.setBook(book);
        hold.setStatus(WaitlistStatus.HOLD_READY);
        hold.setHoldExpiresAt(LocalDateTime.now().plusHours(5));

        memberAuth = new UsernamePasswordAuthenticationToken("alice", null, List.of());
        when(appSharedService.getUserFromAuthForUpdate(memberAuth)).thenReturn(member);
        when(waitlistService.getEntryLocked(50L)).thenReturn(hold);
        when(loanRepository.countByUserIdAndStatusIn(any(), anyCollection())).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(call -> {
            Loan l = call.getArgument(0);
            l.setId(100L);
            return l;
        });
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void claimHold_validHold_createsLoanAndFulfilsTheEntry() {
        LoanResponse response = loanService.claimHold(50L, 7, memberAuth);

        assertEquals(LoanStatus.BORROWED, response.status());
        assertEquals(WaitlistStatus.FULFILLED, hold.getStatus());
        verify(auditLogService).log("WAITLIST", "CLAIMED", 50L);
    }

    @Test
    void claimHold_locksTheMemberBeforeTheBook() {
        loanService.claimHold(50L, 7, memberAuth);

        InOrder order = inOrder(appSharedService, waitlistService);
        order.verify(appSharedService).getUserFromAuthForUpdate(memberAuth);
        order.verify(waitlistService).getEntryLocked(50L);
    }

    @Test
    void claimHold_holdExpired_throws() {
        hold.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));

        InvalidStateException e = assertThrows(InvalidStateException.class,
                () -> loanService.claimHold(50L, 7, memberAuth));
        assertEquals("HOLD_EXPIRED", e.getCode());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void claimHold_noHold_throws() {
        hold.setStatus(WaitlistStatus.WAITING);

        InvalidStateException e = assertThrows(InvalidStateException.class,
                () -> loanService.claimHold(50L, 7, memberAuth));
        assertEquals("NO_ACTIVE_HOLD", e.getCode());
    }

    @Test
    void claimHold_alreadyClaimed_throws() {
        hold.setStatus(WaitlistStatus.FULFILLED);

        InvalidStateException e = assertThrows(InvalidStateException.class,
                () -> loanService.claimHold(50L, 7, memberAuth));
        assertEquals("NO_ACTIVE_HOLD", e.getCode());
    }

    @Test
    void claimHold_holdOfAnotherMember_throwsAccessDenied() {
        Authentication other = new UsernamePasswordAuthenticationToken("bob", null, List.of());
        when(appSharedService.getUserFromAuthForUpdate(other)).thenReturn(new User());

        assertThrows(AccessDeniedException.class, () -> loanService.claimHold(50L, 7, other));
        assertEquals(WaitlistStatus.HOLD_READY, hold.getStatus());
    }

    @Test
    void claimHold_fiveBooksAlreadyOnLoan_throws() {
        when(loanRepository.countByUserIdAndStatusIn(any(), anyCollection())).thenReturn(5L);

        InvalidStateException e = assertThrows(InvalidStateException.class,
                () -> loanService.claimHold(50L, 7, memberAuth));
        assertEquals("LOAN_LIMIT_REACHED", e.getCode());
        assertEquals(WaitlistStatus.HOLD_READY, hold.getStatus());
    }

    @Test
    void claimHold_badDuration_throws() {
        assertThrows(InvalidRequestException.class, () -> loanService.claimHold(50L, 31, memberAuth));
        assertThrows(InvalidRequestException.class, () -> loanService.claimHold(50L, 0, memberAuth));
    }
}
