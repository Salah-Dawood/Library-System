package com.salah.booknest.lending.support;

import com.salah.booknest.lending.model.Fine;
import com.salah.booknest.lending.model.Inventory;
import com.salah.booknest.lending.model.Loan;
import com.salah.booknest.lending.model.LoanStatus;
import com.salah.booknest.lending.model.Member;
import com.salah.booknest.lending.model.WaitlistEntry;
import com.salah.booknest.lending.model.WaitlistStatus;
import com.salah.booknest.lending.repository.FineRepository;
import com.salah.booknest.lending.repository.InventoryRepository;
import com.salah.booknest.lending.repository.LoanRepository;
import com.salah.booknest.lending.repository.MemberRepository;
import com.salah.booknest.lending.repository.WaitlistRepository;
import com.salah.booknest.lending.security.TokenUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Component
public class TestData {

    // users and books get their ids from other services, so the tests hand out their own
    private static final AtomicLong NEXT_ID = new AtomicLong(1000);

    @Autowired private MemberRepository memberRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private FineRepository fineRepository;
    @Autowired private WaitlistRepository waitlistRepository;

    @Value("${jwt-secret}")
    private String jwtSecret;

    private String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public TestUser newUser() {
        return newUserWithRole("member");
    }

    public TestUser newLibrarian() {
        return newUserWithRole("librarian");
    }

    private TestUser newUserWithRole(String role) {
        Member member = memberRepository.save(new Member(NEXT_ID.incrementAndGet(), "user" + unique()));
        return new TestUser(member.getId(), member.getUsername(), role);
    }

    public TestBook newBook(int copies) {
        Inventory inventory = new Inventory();
        inventory.setBookId(NEXT_ID.incrementAndGet());
        inventory.setBookTitle("Book " + unique());
        inventory.setTotalCopies(copies);
        inventory.setAvailableCopies(copies);
        inventoryRepository.save(inventory);
        return new TestBook(inventory.getBookId(), inventory.getBookTitle());
    }

    // a loan that is already out, so one copy is taken from the shelf
    public Loan newLoan(TestUser user, TestBook book) {
        Inventory inventory = inventory(book);
        inventory.setAvailableCopies(inventory.getAvailableCopies() - 1);
        inventoryRepository.save(inventory);

        Loan loan = new Loan();
        loan.setUserId(user.id());
        loan.setUsername(user.username());
        loan.setBookId(book.id());
        loan.setBookTitle(book.title());
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

    public WaitlistEntry newWaitingEntry(TestUser user, TestBook book) {
        return waitlistRepository.save(newEntry(user, book));
    }

    // a copy that is held for this member, the hold ends the given number of minutes from now
    public WaitlistEntry newHoldEntry(TestUser user, TestBook book, int minutesLeft) {
        WaitlistEntry entry = newEntry(user, book);
        entry.setStatus(WaitlistStatus.HOLD_READY);
        entry.setHoldExpiresAt(LocalDateTime.now().plusMinutes(minutesLeft));
        return waitlistRepository.save(entry);
    }

    private WaitlistEntry newEntry(TestUser user, TestBook book) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setUserId(user.id());
        entry.setUsername(user.username());
        entry.setBookId(book.id());
        entry.setBookTitle(book.title());
        return entry;
    }

    public WaitlistEntry reload(WaitlistEntry entry) {
        return waitlistRepository.findById(entry.getId()).get();
    }

    /** The same login the JWT filter builds from a token. */
    public Authentication auth(TestUser user) {
        return new UsernamePasswordAuthenticationToken(new TokenUser(user.id(), user.username()), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
    }

    /** A token like the ones user-service issues, for the tests that go through HTTP. */
    public String token(TestUser user) {
        return Jwts.builder()
                .setSubject(user.username())
                .claim("userId", user.id())
                .claim("role", user.role())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    public Inventory inventory(TestBook book) {
        return inventoryRepository.findByBookId(book.id()).get();
    }

    public void setAvailable(TestBook book, int available) {
        Inventory inventory = inventory(book);
        inventory.setAvailableCopies(available);
        inventoryRepository.save(inventory);
    }

    public long countLoans(TestBook book, LoanStatus status) {
        return loanRepository.findAll().stream()
                .filter(l -> l.getBookId().equals(book.id()) && l.getStatus() == status)
                .count();
    }

    // total copies = on the shelf + out on loan + held for someone in the waitlist
    public void assertStockConsistent(TestBook book) {
        Inventory inventory = inventory(book);
        long onLoan = loanRepository.findAll().stream()
                .filter(l -> l.getBookId().equals(book.id()))
                .filter(l -> l.getStatus() == LoanStatus.BORROWED || l.getStatus() == LoanStatus.PENDING_REVIEW)
                .count();
        long held = waitlistRepository.countByBookIdAndStatus(book.id(), WaitlistStatus.HOLD_READY);
        assertTrue(inventory.getAvailableCopies() >= 0, "available copies went below zero");
        assertEquals((long) inventory.getTotalCopies(), inventory.getAvailableCopies() + onLoan + held,
                "total copies must equal shelf + on loan + held");
    }
}
