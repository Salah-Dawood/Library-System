package com.salah.booknest.lending.cucumber;

import com.salah.booknest.lending.support.TestBook;
import com.salah.booknest.lending.support.TestData;
import com.salah.booknest.lending.support.TestUser;
import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * The members, librarians, books and loans of one scenario, by the names the feature files use.
 * Every name becomes a fresh member or book with its own id, so scenarios never see each other's data.
 * A new one is made for every scenario.
 */
@Component
@ScenarioScope
public class LendingState {

    @Autowired private TestData data;

    private final Map<String, TestUser> users = new HashMap<>();
    private final Map<String, TestBook> books = new HashMap<>();
    private final Map<String, Long> loans = new HashMap<>();

    /** The member with this name, created the first time it is used. */
    public TestUser member(String name) {
        return users.computeIfAbsent(name, key -> data.newUser());
    }

    public TestUser librarian() {
        return users.computeIfAbsent("the librarian", key -> data.newLibrarian());
    }

    public String token(String name) {
        return data.token(name.equals("the librarian") ? librarian() : member(name));
    }

    public void addBook(String title, int copies) {
        books.put(title, data.newBook(copies));
    }

    public TestBook book(String title) {
        TestBook book = books.get(title);
        if (book == null) {
            throw new IllegalStateException("The library has no \"" + title + "\" in this scenario");
        }
        return book;
    }

    public void rememberLoan(String member, String title, long loanId) {
        loans.put(member + "/" + title, loanId);
    }

    public long loan(String member, String title) {
        Long id = loans.get(member + "/" + title);
        if (id == null) {
            throw new IllegalStateException(member + " has no loan of \"" + title + "\" in this scenario");
        }
        return id;
    }
}
