package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.User;
import com.salah.booknest.model.WaitlistEntry;
import com.salah.booknest.model.WaitlistStatus;
import com.salah.booknest.model.request.WaitlistRequest;
import com.salah.booknest.model.response.NotificationEvent;
import com.salah.booknest.model.response.WaitlistResponse;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.InventoryRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.WaitlistRepository;
import com.salah.booknest.security.Roles;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final LoanRepository loanRepository;
    private final AuditLogService auditLogService;
    private final AppSharedService appSharedService;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    @Value("${app.holds.hours:24}")
    private int holdHours;

    @Transactional
    public WaitlistResponse join(Authentication authentication, WaitlistRequest request) {
        User user = appSharedService.getUserFromAuth(authentication);
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new InformationNotFoundException("Book with ID " + request.bookId() + " not found"));
        if (!book.getActive()) {
            throw new InvalidStateException("This book is no longer available");
        }

        // same lock that borrow uses, so a copy can't be returned while we check
        Inventory inventory = getInventoryForUpdate(book.getId());
        if (inventory.getAvailableCopies() > 0) {
            throw new InvalidStateException("Copies are available, borrow the book instead");
        }
        if (loanRepository.existsByUserIdAndBookIdAndStatusIn(user.getId(), book.getId(), List.of(LoanStatus.BORROWED))) {
            throw new InformationExistException("You already have this book on loan");
        }
        if (waitlistRepository.existsByUserIdAndBookIdAndStatusIn(user.getId(), book.getId(), ACTIVE_STATUSES)) {
            throw new InformationExistException("You are already on the waitlist for this book");
        }
        if (waitlistRepository.countByUserIdAndStatus(user.getId(), WaitlistStatus.WAITING) >= MAX_WAITING) {
            throw new InvalidStateException("You can wait for at most " + MAX_WAITING + " books");
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(user);
        entry.setBook(book);
        entry = waitlistRepository.save(entry);

        auditLogService.log("WAITLIST", "JOINED", entry.getId());
        int position = (int) waitlistRepository.countByBookIdAndStatus(book.getId(), WaitlistStatus.WAITING);
        log.info("User {} joined the waitlist of book {} at position {}", user.getId(), book.getId(), position);
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
            offerCopy(getInventoryForUpdate(entry.getBook().getId()));
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
        User user = appSharedService.getUserFromAuth(authentication);
        List<WaitlistEntry> entries =
                waitlistRepository.findAllByUserIdAndStatusInOrderByIdAsc(user.getId(), ACTIVE_STATUSES);
        return entries.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> getQueueForBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
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
        List<WaitlistEntry> entries = waitlistRepository.findAllByBookIdAndStatus(bookId, WaitlistStatus.WAITING);
        for (WaitlistEntry entry : entries) {
            entry.setStatus(WaitlistStatus.CANCELLED);
        }
    }

    // a copy came back: hold it for the first person in line, or put it back on the shelf
    @Transactional
    public void offerCopy(Inventory inventory) {
        Long bookId = inventory.getBook().getId();
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
        log.info("Copy of book {} is held for user {} until {}", bookId, entry.getUser().getId(), entry.getHoldExpiresAt());
        notifyMember(entry, "HOLD_READY", "A copy of \"" + entry.getBook().getTitle()
                + "\" is held for you for " + holdHours + " hours. Claim it before it expires");
    }

    @Transactional(readOnly = true)
    public List<Long> getExpiredHoldIds() {
        List<WaitlistEntry> expired =
                waitlistRepository.findAllByStatusAndHoldExpiresAtBefore(WaitlistStatus.HOLD_READY, LocalDateTime.now());
        return expired.stream().map(WaitlistEntry::getId).toList();
    }

    // the member did not claim in time: the copy goes to the next person in line
    @Transactional
    public void expireHold(Long entryId) {
        WaitlistEntry entry = getEntryLocked(entryId);
        if (entry.getStatus() != WaitlistStatus.HOLD_READY || entry.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
            return;
        }

        entry.setStatus(WaitlistStatus.EXPIRED);
        auditLogService.log("WAITLIST", "EXPIRED", entryId);
        log.info("Hold {} expired for user {}", entryId, entry.getUser().getId());
        notifyMember(entry, "HOLD_EXPIRED", "Your hold on \"" + entry.getBook().getTitle() + "\" expired");
        offerCopy(getInventoryForUpdate(entry.getBook().getId()));
    }

    // lock the book first, then reload the entry, so we always see its latest status
    @Transactional
    public WaitlistEntry getEntryLocked(Long entryId) {
        WaitlistEntry entry = findEntry(entryId);
        getInventoryForUpdate(entry.getBook().getId());
        entityManager.refresh(entry);
        return entry;
    }

    private Inventory getInventoryForUpdate(Long bookId) {
        return inventoryRepository.findByBookIdForUpdate(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Inventory for book " + bookId + " not found"));
    }

    private WaitlistEntry findEntry(Long entryId) {
        return waitlistRepository.findById(entryId)
                .orElseThrow(() -> new InformationNotFoundException("Waitlist entry with ID " + entryId + " not found"));
    }

    private void checkOwner(WaitlistEntry entry, Authentication authentication) {
        boolean owner = entry.getUser().getUsername().equals(authentication.getName());
        if (!owner && !Roles.isLibrarian(authentication)) {
            throw new AccessDeniedException("This is not your waitlist entry");
        }
    }

    private void notifyMember(WaitlistEntry entry, String type, String message) {
        String username = entry.getUser().getUsername();
        NotificationEvent event = new NotificationEvent(type, message, null, null, LocalDateTime.now());
        afterCommit(() -> notificationService.notifyUser(username, event));
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    // position 1 means next in line, 0 means not waiting anymore
    private WaitlistResponse toResponse(WaitlistEntry entry) {
        if (entry.getStatus() != WaitlistStatus.WAITING) {
            return WaitlistResponse.from(entry, 0);
        }
        long ahead = waitlistRepository.countByBookIdAndStatusAndIdLessThan(
                entry.getBook().getId(), WaitlistStatus.WAITING, entry.getId());
        return WaitlistResponse.from(entry, (int) ahead + 1);
    }
}
