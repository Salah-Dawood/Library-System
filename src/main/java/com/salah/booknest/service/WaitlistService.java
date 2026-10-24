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
import com.salah.booknest.model.response.WaitlistResponse;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.InventoryRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.WaitlistRepository;
import com.salah.booknest.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistService {

    private static final int MAX_WAITING = 5;

    private final WaitlistRepository waitlistRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final LoanRepository loanRepository;
    private final AuditLogService auditLogService;
    private final AppSharedService appSharedService;

    @Transactional
    public WaitlistResponse join(Authentication authentication, WaitlistRequest request) {
        User user = appSharedService.getUserFromAuth(authentication);
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new InformationNotFoundException("Book with ID " + request.bookId() + " not found"));
        if (!book.getActive()) {
            throw new InvalidStateException("This book is no longer available");
        }

        // same lock that borrow uses, so a copy can't be returned while we check
        Inventory inventory = inventoryRepository.findByBookIdForUpdate(book.getId())
                .orElseThrow(() -> new InformationNotFoundException("Inventory for book " + book.getId() + " not found"));
        if (inventory.getAvailableCopies() > 0) {
            throw new InvalidStateException("Copies are available, borrow the book instead");
        }
        if (loanRepository.existsByUserIdAndBookIdAndStatusIn(user.getId(), book.getId(), List.of(LoanStatus.BORROWED))) {
            throw new InformationExistException("You already have this book on loan");
        }
        if (waitlistRepository.existsByUserIdAndBookIdAndStatus(user.getId(), book.getId(), WaitlistStatus.WAITING)) {
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
        WaitlistEntry entry = findEntry(entryId);
        checkOwner(entry, authentication);
        if (entry.getStatus() != WaitlistStatus.WAITING) {
            throw new InvalidStateException("You are not waiting for this book anymore");
        }

        entry.setStatus(WaitlistStatus.CANCELLED);
        auditLogService.log("WAITLIST", "LEFT", entryId);
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
                waitlistRepository.findAllByUserIdAndStatusOrderByIdAsc(user.getId(), WaitlistStatus.WAITING);
        return entries.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> getQueueForBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new InformationNotFoundException("Book with ID " + bookId + " not found");
        }
        List<WaitlistEntry> entries =
                waitlistRepository.findAllByBookIdAndStatusOrderByIdAsc(bookId, WaitlistStatus.WAITING);
        List<WaitlistResponse> queue = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            queue.add(WaitlistResponse.from(entries.get(i), i + 1));
        }
        return queue;
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
