package com.salah.booknest.lending.config;

import com.salah.booknest.lending.model.Inventory;
import com.salah.booknest.lending.model.Member;
import com.salah.booknest.lending.repository.InventoryRepository;
import com.salah.booknest.lending.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seeds lending's copy of the sample users and the stock of the sample books.
 * user-service seeds the same users and catalog-service the same books, in the same order,
 * so on empty databases the ids match (users 1 to 4, books 1 to 16).
 */
@Component
@Profile("!test")
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private static final List<String> MEMBERS = List.of("librarian", "johnd", "janed", "alexm");

    private static final List<String> BOOK_TITLES = List.of(
            "1984",
            "Animal Farm",
            "Harry Potter and the Sorcerer's Stone",
            "Harry Potter and the Order of the Phoenix",
            "The Hobbit",
            "The Fellowship of the Ring",
            "The Two Towers",
            "To Kill a Mockingbird",
            "All Quiet on the Western Front",
            "The Road Back",
            "Foundation",
            "I, Robot",
            "Murder on the Orient Express",
            "And Then There Were None",
            "Dune",
            "Dune Messiah");

    private final MemberRepository memberRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (inventoryRepository.count() > 0) {
            log.info("Stock already exists. Skipping seeding.");
            return;
        }
        for (int i = 0; i < MEMBERS.size(); i++) {
            memberRepository.save(new Member((long) i + 1, MEMBERS.get(i)));
        }
        for (int i = 0; i < BOOK_TITLES.size(); i++) {
            seedStock((long) i + 1, BOOK_TITLES.get(i));
        }
        log.info("Members and stock successfully seeded!");
    }

    private void seedStock(Long bookId, String title) {
        int copies = ThreadLocalRandom.current().nextInt(5, 20);
        Inventory inventory = new Inventory();
        inventory.setBookId(bookId);
        inventory.setBookTitle(title);
        inventory.setTotalCopies(copies);
        inventory.setAvailableCopies(copies);
        inventoryRepository.save(inventory);
    }
}
