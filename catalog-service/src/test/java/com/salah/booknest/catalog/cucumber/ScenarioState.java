package com.salah.booknest.catalog.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * What one scenario has set up so far: who is logged in, and the ids of the authors, genres and books
 * it created by name. A new one is made for every scenario.
 */
@Component
@ScenarioScope
public class ScenarioState {

    private static final AtomicInteger ISBN_COUNTER = new AtomicInteger(1000);

    private String token;
    private final Map<String, Long> authors = new HashMap<>();
    private final Map<String, Long> genres = new HashMap<>();
    private final Map<String, Long> books = new HashMap<>();

    public String token() {
        if (token == null) {
            throw new IllegalStateException("Nobody has logged in in this scenario");
        }
        return token;
    }

    public void logIn(String token) {
        this.token = token;
    }

    public Map<String, Long> authors() {
        return authors;
    }

    public Map<String, Long> genres() {
        return genres;
    }

    public long book(String title) {
        Long id = books.get(title);
        if (id == null) {
            throw new IllegalStateException("No book \"" + title + "\" was added in this scenario");
        }
        return id;
    }

    public void rememberBook(String title, long id) {
        books.put(title, id);
    }

    /** A valid ISBN-13 that no other scenario uses. */
    public static String newIsbn() {
        String first12 = "978" + String.format("%09d", ISBN_COUNTER.incrementAndGet());
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (first12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return first12 + (10 - sum % 10) % 10;
    }
}
