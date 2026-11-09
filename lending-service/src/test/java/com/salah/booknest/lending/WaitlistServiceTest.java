package com.salah.booknest.lending;

import com.salah.booknest.lending.exception.InformationExistException;
import com.salah.booknest.lending.exception.InvalidStateException;
import com.salah.booknest.lending.model.Inventory;
import com.salah.booknest.lending.model.Member;
import com.salah.booknest.lending.model.WaitlistEntry;
import com.salah.booknest.lending.model.WaitlistStatus;
import com.salah.booknest.lending.model.request.WaitlistRequest;
import com.salah.booknest.lending.model.response.WaitlistResponse;
import com.salah.booknest.lending.repository.InventoryRepository;
import com.salah.booknest.lending.repository.LoanRepository;
import com.salah.booknest.lending.repository.WaitlistRepository;
import com.salah.booknest.lending.security.TokenUser;
import com.salah.booknest.lending.service.AuditLogService;
import com.salah.booknest.lending.service.InventoryLocker;
import com.salah.booknest.lending.service.MemberService;
import com.salah.booknest.lending.event.LendingEventPublisher;
import com.salah.booknest.lending.service.WaitlistService;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WaitlistServiceTest {

    @Mock private WaitlistRepository waitlistRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryLocker inventoryLocker;
    @Mock private LoanRepository loanRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private MemberService memberService;
    @Mock private LendingEventPublisher eventPublisher;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private WaitlistService waitlistService;

    private Member member;
    private Member otherMember;
    private Inventory inventory;
    private Authentication memberAuth;
    private Authentication otherAuth;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        ReflectionTestUtils.setField(waitlistService, "holdHours", 24);

        member = new Member(1L, "alice");
        otherMember = new Member(2L, "bob");

        inventory = new Inventory();
        inventory.setBookId(10L);
        inventory.setBookTitle("Clean Code");
        inventory.setActive(true);
        inventory.setTotalCopies(1);
        inventory.setAvailableCopies(0);

        memberAuth = auth(member);
        otherAuth = auth(otherMember);
        when(memberService.lockCurrentMember(memberAuth)).thenReturn(member);
        when(inventoryRepository.findByBookId(10L)).thenReturn(Optional.of(inventory));
        when(inventoryLocker.lock(10L)).thenReturn(inventory);
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenAnswer(call -> {
            WaitlistEntry e = call.getArgument(0);
            e.setId(50L);
            return e;
        });
        when(waitlistRepository.existsByUserIdAndBookIdAndStatusIn(anyLong(), anyLong(), anyList()))
                .thenReturn(false);
        when(waitlistRepository.countByUserIdAndStatus(anyLong(), eq(WaitlistStatus.WAITING))).thenReturn(0L);
        when(waitlistRepository.countByBookIdAndStatus(10L, WaitlistStatus.WAITING)).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // join

    @Test
    void join_noCopiesAvailable_addsMemberToTheQueue() {
        WaitlistResponse response = waitlistService.join(memberAuth, new WaitlistRequest(10L));

        assertEquals(WaitlistStatus.WAITING, response.status());
        assertEquals(1, response.position());
        verify(waitlistRepository).save(any(WaitlistEntry.class));
    }

    @Test
    void join_copiesAvailable_throws() {
        inventory.setAvailableCopies(1);

        InvalidStateException e = assertThrows(InvalidStateException.class,
                () -> waitlistService.join(memberAuth, new WaitlistRequest(10L)));
        assertEquals("COPIES_AVAILABLE", e.getCode());
        verify(waitlistRepository, never()).save(any(WaitlistEntry.class));
    }

    @Test
    void join_alreadyWaiting_throws() {
        when(waitlistRepository.existsByUserIdAndBookIdAndStatusIn(eq(1L), eq(10L), anyList()))
                .thenReturn(true);

        assertThrows(InformationExistException.class,
                () -> waitlistService.join(memberAuth, new WaitlistRequest(10L)));
    }

    @Test
    void join_alreadyHasTheBookOnLoan_throws() {
        when(loanRepository.existsByUserIdAndBookIdAndStatusIn(eq(1L), eq(10L), anyCollection()))
                .thenReturn(true);

        assertThrows(InformationExistException.class,
                () -> waitlistService.join(memberAuth, new WaitlistRequest(10L)));
    }

    @Test
    void join_fiveBooksAlreadyWaiting_throws() {
        when(waitlistRepository.countByUserIdAndStatus(1L, WaitlistStatus.WAITING)).thenReturn(5L);

        InvalidStateException e = assertThrows(InvalidStateException.class,
                () -> waitlistService.join(memberAuth, new WaitlistRequest(10L)));
        assertEquals("WAITLIST_LIMIT_REACHED", e.getCode());
    }

    @Test
    void join_locksTheMemberBeforeTheBook() {
        waitlistService.join(memberAuth, new WaitlistRequest(10L));

        InOrder order = inOrder(memberService, inventoryLocker);
        order.verify(memberService).lockCurrentMember(memberAuth);
        order.verify(inventoryLocker).lock(10L);
    }

    // offerCopy

    @Test
    void offerCopy_nobodyWaiting_putsCopyOnTheShelf() {
        when(waitlistRepository.findFirstByBookIdAndStatusOrderByIdAsc(10L, WaitlistStatus.WAITING))
                .thenReturn(Optional.empty());

        waitlistService.offerCopy(inventory);

        assertEquals(Integer.valueOf(1), inventory.getAvailableCopies());
    }

    @Test
    void offerCopy_someoneWaiting_holdsTheCopyAndKeepsTheShelfEmpty() {
        WaitlistEntry first = entry(50L, member, WaitlistStatus.WAITING);
        when(waitlistRepository.findFirstByBookIdAndStatusOrderByIdAsc(10L, WaitlistStatus.WAITING))
                .thenReturn(Optional.of(first));

        waitlistService.offerCopy(inventory);

        assertEquals(WaitlistStatus.HOLD_READY, first.getStatus());
        assertTrue(first.getHoldExpiresAt().isAfter(LocalDateTime.now().plusHours(23)));
        assertEquals(Integer.valueOf(0), inventory.getAvailableCopies());
    }

    // expireHold

    @Test
    void expireHold_holdTimeIsOver_expiresItAndOffersTheCopyOnward() {
        WaitlistEntry held = entry(50L, member, WaitlistStatus.HOLD_READY);
        held.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(waitlistRepository.findById(50L)).thenReturn(Optional.of(held));
        when(waitlistRepository.findFirstByBookIdAndStatusOrderByIdAsc(10L, WaitlistStatus.WAITING))
                .thenReturn(Optional.empty());

        boolean expired = waitlistService.expireHold(50L);

        assertTrue(expired);
        assertEquals(WaitlistStatus.EXPIRED, held.getStatus());
        assertEquals(Integer.valueOf(1), inventory.getAvailableCopies());
    }

    @Test
    void expireHold_holdTimeNotOverYet_doesNothing() {
        WaitlistEntry held = entry(50L, member, WaitlistStatus.HOLD_READY);
        held.setHoldExpiresAt(LocalDateTime.now().plusHours(1));
        when(waitlistRepository.findById(50L)).thenReturn(Optional.of(held));

        assertFalse(waitlistService.expireHold(50L));
        assertEquals(WaitlistStatus.HOLD_READY, held.getStatus());
    }

    @Test
    void expireHold_alreadyClaimed_doesNothing() {
        WaitlistEntry claimed = entry(50L, member, WaitlistStatus.FULFILLED);
        claimed.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(waitlistRepository.findById(50L)).thenReturn(Optional.of(claimed));

        assertFalse(waitlistService.expireHold(50L));
        assertEquals(WaitlistStatus.FULFILLED, claimed.getStatus());
        assertEquals(Integer.valueOf(0), inventory.getAvailableCopies());
    }

    // leave

    @Test
    void leave_whileWaiting_cancelsTheEntry() {
        WaitlistEntry waiting = entry(50L, member, WaitlistStatus.WAITING);
        when(waitlistRepository.findById(50L)).thenReturn(Optional.of(waiting));

        waitlistService.leave(50L, memberAuth);

        assertEquals(WaitlistStatus.CANCELLED, waiting.getStatus());
        assertEquals(Integer.valueOf(0), inventory.getAvailableCopies());
    }

    @Test
    void leave_whileHoldingACopy_passesTheCopyOn() {
        WaitlistEntry held = entry(50L, member, WaitlistStatus.HOLD_READY);
        held.setHoldExpiresAt(LocalDateTime.now().plusHours(5));
        when(waitlistRepository.findById(50L)).thenReturn(Optional.of(held));
        when(waitlistRepository.findFirstByBookIdAndStatusOrderByIdAsc(10L, WaitlistStatus.WAITING))
                .thenReturn(Optional.empty());

        waitlistService.leave(50L, memberAuth);

        assertEquals(WaitlistStatus.CANCELLED, held.getStatus());
        assertEquals(Integer.valueOf(1), inventory.getAvailableCopies());
    }

    @Test
    void leave_entryOfAnotherMember_throwsAccessDenied() {
        WaitlistEntry waiting = entry(50L, member, WaitlistStatus.WAITING);
        when(waitlistRepository.findById(50L)).thenReturn(Optional.of(waiting));

        assertThrows(AccessDeniedException.class, () -> waitlistService.leave(50L, otherAuth));
        assertEquals(WaitlistStatus.WAITING, waiting.getStatus());
    }

    @Test
    void leave_alreadyCancelled_throws() {
        WaitlistEntry cancelled = entry(50L, member, WaitlistStatus.CANCELLED);
        when(waitlistRepository.findById(50L)).thenReturn(Optional.of(cancelled));

        assertThrows(InvalidStateException.class, () -> waitlistService.leave(50L, memberAuth));
    }

    // helpers

    private Authentication auth(Member member) {
        return new UsernamePasswordAuthenticationToken(new TokenUser(member.getId(), member.getUsername()), null, List.of());
    }

    private WaitlistEntry entry(Long id, Member user, WaitlistStatus status) {
        WaitlistEntry e = new WaitlistEntry();
        e.setId(id);
        e.setUserId(user.getId());
        e.setUsername(user.getUsername());
        e.setBookId(10L);
        e.setBookTitle("Clean Code");
        e.setStatus(status);
        return e;
    }
}