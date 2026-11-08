package com.salah.booknest.lending;

import com.salah.booknest.lending.exception.InvalidRequestException;
import com.salah.booknest.lending.exception.InvalidStateException;
import com.salah.booknest.lending.model.Loan;
import com.salah.booknest.lending.model.LoanStatus;
import com.salah.booknest.lending.model.Member;
import com.salah.booknest.lending.model.WaitlistEntry;
import com.salah.booknest.lending.model.WaitlistStatus;
import com.salah.booknest.lending.model.response.LoanResponse;
import com.salah.booknest.lending.repository.InventoryRepository;
import com.salah.booknest.lending.repository.FineRepository;
import com.salah.booknest.lending.repository.LoanRepository;
import com.salah.booknest.lending.security.TokenUser;
import com.salah.booknest.lending.service.AuditLogService;
import com.salah.booknest.lending.service.InventoryLocker;
import com.salah.booknest.lending.service.LoanService;
import com.salah.booknest.lending.service.MemberService;
import com.salah.booknest.lending.service.NotificationService;
import com.salah.booknest.lending.service.WaitlistService;
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
    private WaitlistEntry hold;
    private Authentication memberAuth;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();

        member = new Member(1L, "alice");

        hold = new WaitlistEntry();
        hold.setId(50L);
        hold.setUserId(1L);
        hold.setUsername("alice");
        hold.setBookId(10L);
        hold.setBookTitle("Clean Code");
        hold.setStatus(WaitlistStatus.HOLD_READY);
        hold.setHoldExpiresAt(LocalDateTime.now().plusHours(5));

        memberAuth = new UsernamePasswordAuthenticationToken(new TokenUser(1L, "alice"), null, List.of());
        when(memberService.lockCurrentMember(memberAuth)).thenReturn(member);
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

        InOrder order = inOrder(memberService, waitlistService);
        order.verify(memberService).lockCurrentMember(memberAuth);
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
        Authentication other = new UsernamePasswordAuthenticationToken(new TokenUser(2L, "bob"), null, List.of());
        when(memberService.lockCurrentMember(other)).thenReturn(new Member(2L, "bob"));

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
