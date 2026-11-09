package com.salah.booknest.lending.service;

import com.salah.booknest.lending.event.LendingEvent;
import com.salah.booknest.lending.event.LendingEventPublisher;
import com.salah.booknest.lending.exception.InformationExistException;
import com.salah.booknest.lending.exception.InformationNotFoundException;
import com.salah.booknest.lending.exception.InvalidStateException;
import com.salah.booknest.lending.model.Inventory;
import com.salah.booknest.lending.model.LoanStatus;
import com.salah.booknest.lending.model.Member;
import com.salah.booknest.lending.model.WaitlistEntry;
import com.salah.booknest.lending.model.WaitlistStatus;
import com.salah.booknest.lending.model.request.WaitlistRequest;
import com.salah.booknest.lending.model.response.WaitlistResponse;
import com.salah.booknest.lending.repository.InventoryRepository;
import com.salah.booknest.lending.repository.LoanRepository;
import com.salah.booknest.lending.repository.WaitlistRepository;
import com.salah.booknest.lending.security.Roles;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistService {

    private static final int MAX_WAITING = 5;
    private static final List<WaitlistStatus> ACTIVE_STATUSES =
            List.of(WaitlistStatus.WAITING, WaitlistStatus.HOLD_READY);

    private final WaitlistRepository waitlistRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryLocker inventoryLocker;
    private final LoanRepository loanRepository;
    private final AuditLogService auditLogService;
    private final MemberService memberService;
    private final LendingEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Value("${app.holds.hours}")
    private int holdHours;

    @Transactional
    public WaitlistResponse join(Authentication authentication, WaitlistRequest request) {
        Member member = memberService.lockCurrentMember(authentication);
        Inventory stock = inventoryRepository.findByBookId(request.bookId())
                .orElseThrow(() -> new InformationNotFoundException("Book with ID " + request.bookId() + " not found"));
        if (!Boolean.TRUE.equals(stock.getActive())) {
            throw new InvalidStateException("This book is no longer available");
        }

        // same lock that borrow uses, so a copy can't be returned while we check
        Inventory inventory = inventoryLocker.lock(stock.getBookId());
        if (inventory.getAvailableCopies() > 0) {
            throw new InvalidStateException("Copies are available, borrow the book instead", "COPIES_AVAILABLE");
        }
        if (loanRepository.existsByUserIdAndBookIdAndStatusIn(member.getId(), inventory.getBookId(), List.of(LoanStatus.BORROWED))) {
            throw new InformationExistException("You already have this book on loan");
        }
        if (waitlistRepository.existsByUserIdAndBookIdAndStatusIn(member.getId(), inventory.getBookId(), ACTIVE_STATUSES)) {
            throw new InformationExistException("You are already on the waitlist for this book");
        }
        if (waitlistRepository.countByUserIdAndStatus(member.getId(), WaitlistStatus.WAITING) >= MAX_WAITING) {
            throw new InvalidStateException("You can wait for at most " + MAX_WAITING + " books", "WAITLIST_LIMIT_REACHED");
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setUserId(member.getId());
        entry.setUsername(member.getUsername());
        entry.setBookId(inventory.getBookId());
        entry.setBookTitle(inventory.getBookTitle());
        entry = waitlistRepository.save(entry);

        auditLogService.log("WAITLIST", "JOINED", entry.getId());
        int position = (int) waitlistRepository.countByBookIdAndStatus(inventory.getBookId(), WaitlistStatus.WAITING);
        log.info("User {} joined the waitlist of book {} at position {}", member.getId(), inventory.getBookId(), position);
        return WaitlistResponse.from(entry, position);
    }

    @Transactional
    public WaitlistResponse leave(Long entryId, Authentication authentication) {
        WaitlistEntry entry = getEntryLocked(entryId);
        checkOwner(entry, authentication);
        WaitlistStatus oldStatus = entry.getStatus();
        if (!ACTIVE_STATUSES.contains(oldStatus)) {
            throw new InvalidStateException("You are not waiting for this book anymore");
        }

        entry.setStatus(WaitlistStatus.CANCELLED);
        auditLogService.log("WAITLIST", "LEFT", entryId);
        if (oldStatus == WaitlistStatus.HOLD_READY) {
            // the copy that was held for this member goes to the next person
            offerCopy(inventoryLocker.lock(entry.getBookId()));
        }
        log.info("Waitlist entry {} was cancelled by {}", entryId, authentication.getName());
        return WaitlistResponse.from(entry, 0);
    }

    @Transactional(readOnly = true)
    public WaitlistResponse getEntry(Long entryId, Authentication authentication) {
        WaitlistEntry entry = findEntry(entryId);
        checkOwner(entry, authentication);
        return toResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> getMyWaitlist(Authentication authentication) {
        List<WaitlistEntry> entries = waitlistRepository.findAllByUserIdAndStatusInOrderByIdAsc(
                MemberService.tokenUser(authentication).id(), ACTIVE_STATUSES);
        return entries.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> getQueueForBook(Long bookId) {
        if (inventoryRepository.findByBookId(bookId).isEmpty()) {
            throw new InformationNotFoundException("Book with ID " + bookId + " not found");
        }
        List<WaitlistEntry> entries =
                waitlistRepository.findAllByBookIdAndStatusInOrderByIdAsc(bookId, ACTIVE_STATUSES);
        List<WaitlistResponse> queue = new ArrayList<>();
        int position = 0;
        for (WaitlistEntry entry : entries) {
            if (entry.getStatus() == WaitlistStatus.WAITING) {
                position++;
                queue.add(WaitlistResponse.from(entry, position));
            } else {
                queue.add(WaitlistResponse.from(entry, 0));
            }
        }
        return queue;
    }

    @Transactional
    public void cancelAllForBook(Long bookId) {
        Inventory inventory = inventoryLocker.lock(bookId);
        List<WaitlistEntry> entries = waitlistRepository.findAllByBookIdAndStatusIn(bookId, ACTIVE_STATUSES);
        for (WaitlistEntry entry : entries) {
            if (entry.getStatus() == WaitlistStatus.HOLD_READY) {
                inventory.setAvailableCopies(Math.min(inventory.getTotalCopies(), inventory.getAvailableCopies() + 1));
            }
            entry.setStatus(WaitlistStatus.CANCELLED);
        }
    }

    // a copy came back: hold it for the first person in line, or put it back on the shelf
    @Transactional
    public void offerCopy(Inventory inventory) {
        Long bookId = inventory.getBookId();
        Optional<WaitlistEntry> first =
                waitlistRepository.findFirstByBookIdAndStatusOrderByIdAsc(bookId, WaitlistStatus.WAITING);
        if (first.isEmpty()) {
            inventory.setAvailableCopies(Math.min(inventory.getTotalCopies(), inventory.getAvailableCopies() + 1));
            return;
        }

        WaitlistEntry entry = first.get();
        entry.setStatus(WaitlistStatus.HOLD_READY);
        entry.setHoldExpiresAt(LocalDateTime.now().plusHours(holdHours));
        auditLogService.log("WAITLIST", "HOLD_STARTED", entry.getId());
        log.info("Copy of book {} is held for user {} until {}", bookId, entry.getUserId(), entry.getHoldExpiresAt());
        notifyMember(entry, "HOLD_READY", "A copy of \"" + entry.getBookTitle()
                + "\" is held for you for " + holdHours + " hours. Claim it before it expires");
    }

    @Transactional(readOnly = true)
    public List<Long> getExpiredHoldIds() {
        List<WaitlistEntry> expired =
                waitlistRepository.findAllByStatusAndHoldExpiresAtBefore(WaitlistStatus.HOLD_READY, LocalDateTime.now());
        return expired.stream().map(WaitlistEntry::getId).toList();
    }

    // the member did not claim in time: the copy goes to the next person in line
    // returns false when there was nothing to do, for example the member claimed it just before
    @Transactional
    public boolean expireHold(Long entryId) {
        WaitlistEntry entry = getEntryLocked(entryId);
        if (entry.getStatus() != WaitlistStatus.HOLD_READY || entry.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
            return false;
        }

        entry.setStatus(WaitlistStatus.EXPIRED);
        auditLogService.log("WAITLIST", "EXPIRED", entryId);
        log.info("Hold {} expired for user {}", entryId, entry.getUserId());
        notifyMember(entry, "HOLD_EXPIRED", "Your hold on \"" + entry.getBookTitle() + "\" expired");
        offerCopy(inventoryLocker.lock(entry.getBookId()));
        return true;
    }

    // lock the book first, then reload the entry, so we always see its latest status
    @Transactional
    public WaitlistEntry getEntryLocked(Long entryId) {
        WaitlistEntry entry = findEntry(entryId);
        inventoryLocker.lock(entry.getBookId());
        entityManager.refresh(entry);
        return entry;
    }

    private WaitlistEntry findEntry(Long entryId) {
        return waitlistRepository.findById(entryId)
                .orElseThrow(() -> new InformationNotFoundException("Waitlist entry with ID " + entryId + " not found"));
    }

    private void checkOwner(WaitlistEntry entry, Authentication authentication) {
        boolean owner = entry.getUsername().equals(authentication.getName());
        if (!owner && !Roles.isLibrarian(authentication)) {
            throw new AccessDeniedException("This is not your waitlist entry");
        }
    }

    private void notifyMember(WaitlistEntry entry, String type, String message) {
        eventPublisher.publishAfterCommit(LendingEventPublisher.WAITLIST_EVENTS, LendingEvent.forMember(
                type, entry.getUserId(), entry.getUsername(), null, null, message));
    }

    // position 1 means next in line, 0 means not waiting anymore
    private WaitlistResponse toResponse(WaitlistEntry entry) {
        if (entry.getStatus() != WaitlistStatus.WAITING) {
            return WaitlistResponse.from(entry, 0);
        }
        long ahead = waitlistRepository.countByBookIdAndStatusAndIdLessThan(
                entry.getBookId(), WaitlistStatus.WAITING, entry.getId());
        return WaitlistResponse.from(entry, (int) ahead + 1);
    }
}
