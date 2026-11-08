package com.salah.booknest.support;

import com.salah.booknest.model.Author;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Fine;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.User;
import com.salah.booknest.model.WaitlistEntry;
import com.salah.booknest.model.WaitlistStatus;
import com.salah.booknest.repository.AuthorRepository;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.FineRepository;
import com.salah.booknest.repository.InventoryRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.repository.WaitlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Component
public class TestData {

    @Autowired private UserRepository userRepository;
    @Autowired private AuthorRepository authorRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private FineRepository fineRepository;
    @Autowired private WaitlistRepository waitlistRepository;

    private String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public User newUser() {
        return newUserWithRole("Member");
    }

    public User newLibrarian() {
        return newUserWithRole("Librarian");
    }

    private User newUserWithRole(String role) {
        String name = "user" + unique();
        User user = new User();
        user.setUsername(name);
        user.setEmailAddress(name + "@test.com");
        user.setPassword("password");
        user.setRole(role);
        user.setIsVerified(true);
        return userRepository.save(user);
    }

    public Book newBook(int copies) {
        Author author = new Author();
        author.setName("Author " + unique());
        author.setBirthYear(1970);
        author.setNationality("Test");
        authorRepository.save(author);

        Book book = new Book();
        book.setTitle("Book " + unique());
        book.setAuthor(author);
        book.setIsbn(unique() + unique());
        book.setPublishedYear(2020);
        bookRepository.save(book);

        Inventory inventory = new Inventory();
        inventory.setBook(book);
        inventory.setTotalCopies(copies);
        inventory.setAvailableCopies(copies);
        inventoryRepository.save(inventory);
        return book;
    }

    // a loan that is already out, so one copy is taken from the shelf
    public Loan newLoan(User user, Book book) {
        Inventory inventory = inventoryRepository.findByBookId(book.getId()).get();
        inventory.setAvailableCopies(inventory.getAvailableCopies() - 1);
        inventoryRepository.save(inventory);

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setBook(book);
        loan.setStatus(LoanStatus.BORROWED);
        loan.setRequestedDays(7);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(7));
        return loanRepository.save(loan);
    }

    public Fine newFine(Loan loan) {
        Fine fine = new Fine();
        fine.setLoan(loan);
        fine.setAmount(new BigDecimal("2.00"));
        fine.setReason("Test fine");
        return fineRepository.save(fine);
    }

    public WaitlistEntry newWaitingEntry(User user, Book book) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(user);
        entry.setBook(book);
        return waitlistRepository.save(entry);
    }

    // a copy that is held for this member, the hold ends the given number of minutes from now
    public WaitlistEntry newHoldEntry(User user, Book book, int minutesLeft) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(user);
        entry.setBook(book);
        entry.setStatus(WaitlistStatus.HOLD_READY);
        entry.setHoldExpiresAt(LocalDateTime.now().plusMinutes(minutesLeft));
        return waitlistRepository.save(entry);
    }

    public WaitlistEntry reload(WaitlistEntry entry) {
        return waitlistRepository.findById(entry.getId()).get();
    }

    public Authentication auth(User user) {
        List<SimpleGrantedAuthority> roles = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toLowerCase()));
        return new UsernamePasswordAuthenticationToken(user.getUsername(), null, roles);
    }

    public Inventory inventory(Book book) {
        return inventoryRepository.findByBookId(book.getId()).get();
    }

    public void setAvailable(Book book, int available) {
        Inventory inventory = inventoryRepository.findByBookId(book.getId()).get();
        inventory.setAvailableCopies(available);
        inventoryRepository.save(inventory);
    }

    public long countLoans(Book book, LoanStatus status) {
        return loanRepository.findAll().stream()
                .filter(l -> l.getBook().getId().equals(book.getId()) && l.getStatus() == status)
                .count();
    }

    // total copies = on the shelf + out on loan + held for someone in the waitlist
    public void assertStockConsistent(Book book) {
        Inventory inventory = inventory(book);
        long onLoan = loanRepository.findAll().stream()
                .filter(l -> l.getBook().getId().equals(book.getId()))
                .filter(l -> l.getStatus() == LoanStatus.BORROWED || l.getStatus() == LoanStatus.PENDING_REVIEW)
                .count();
        long held = waitlistRepository.countByBookIdAndStatus(book.getId(), WaitlistStatus.HOLD_READY);
        assertTrue(inventory.getAvailableCopies() >= 0, "available copies went below zero");
        assertEquals((long) inventory.getTotalCopies(), inventory.getAvailableCopies() + onLoan + held,
                "total copies must equal shelf + on loan + held");
    }
}