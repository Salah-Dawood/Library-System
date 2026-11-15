package com.salah.booknest.catalog.cucumber;

import com.salah.booknest.catalog.model.Author;
import com.salah.booknest.catalog.model.Genre;
import com.salah.booknest.catalog.repository.AuthorRepository;
import com.salah.booknest.catalog.repository.BookRepository;
import com.salah.booknest.catalog.repository.GenreRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of books.feature: adding, listing, changing and removing books, and what lending-service sees.
 */
public class BookSteps {

    @Autowired private ApiClient api;
    @Autowired private ScenarioState state;
    @Autowired private Tokens tokens;
    @Autowired private AuthorRepository authorRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private BookRepository bookRepository;

    @Given("the catalogue has the author {string} and the genre {string}")
    public void authorAndGenre(String authorName, String genreName) {
        Author author = new Author();
        author.setName(authorName);
        author.setBirthYear(1920);
        author.setNationality("USA");
        state.authors().put(authorName, authorRepository.save(author).getId());

        Genre genre = new Genre();
        genre.setName(genreName);
        genre.setDescription("Books about " + genreName);
        state.genres().put(genreName, genreRepository.save(genre).getId());
    }

    @Given("I am logged in as the librarian")
    public void loggedInAsLibrarian() {
        state.logIn(tokens.librarian());
    }

    @Given("I am logged in as the member {string}")
    public void loggedInAsMember(String username) {
        state.logIn(tokens.member(username));
    }

    @Given("the librarian has added the book {string} with {int} copies")
    public void librarianAdded(String title, int copies) {
        librarianAddedWithIsbn(title, ScenarioState.newIsbn(), copies);
    }

    @Given("the librarian has added the book {string} with ISBN {string} and {int} copies")
    public void librarianAddedWithIsbn(String title, String isbn, int copies) {
        api.send("POST", "/api/books", tokens.librarian(), bookJson(title, isbn, true, copies));
        assertEquals(201, api.lastStatus(), "could not add the book: " + api.lastBody());
        state.rememberBook(title, api.lastJson().get("id").asLong());
    }

    @Given("{int} copies of {string} are on loan")
    public void copiesOnLoan(int copies, String title) {
        FakeLending.INSTANCE.lend(state.book(title), copies);
    }

    @When("I add the book {string} with {int} copies")
    public void addBook(String title, int copies) {
        addBookWithIsbn(title, ScenarioState.newIsbn(), copies);
    }

    @When("I add the book {string} with ISBN {string} and {int} copies")
    public void addBookWithIsbn(String title, String isbn, int copies) {
        api.send("POST", "/api/books", state.token(), bookJson(title, isbn, true, copies));
        if (api.lastStatus() == 201) {
            state.rememberBook(title, api.lastJson().get("id").asLong());
        }
    }

    @When("I add the book {string} without any genre")
    public void addBookWithoutGenre(String title) {
        api.send("POST", "/api/books", state.token(), bookJson(title, ScenarioState.newIsbn(), false, 1));
    }

    @When("I search the books for {string}")
    public void searchBooks(String text) {
        api.send("GET", "/api/books?title=" + URLEncoder.encode(text, StandardCharsets.UTF_8), state.token(), null);
    }

    @When("I open the book {string}")
    public void openBook(String title) {
        api.send("GET", "/api/books/" + state.book(title), state.token(), null);
    }

    @When("I change the copies of {string} to {int}")
    public void changeCopies(String title, int copies) {
        String isbn = bookRepository.findById(state.book(title)).orElseThrow().getIsbn();
        api.send("PUT", "/api/books/" + state.book(title), state.token(), bookJson(title, isbn, true, copies));
    }

    @When("I remove the book {string}")
    public void removeBook(String title) {
        api.send("DELETE", "/api/books/" + state.book(title), state.token(), null);
    }

    @Then("lending-service holds {int} copies of {string}")
    public void lendingHolds(int copies, String title) {
        assertEquals(copies, FakeLending.INSTANCE.totalCopies(state.book(title)));
    }

    @Then("lending-service holds no copies of {string}")
    public void lendingHoldsNone(String title) {
        assertNull(FakeLending.INSTANCE.totalCopies(state.book(title)));
    }

    @Then("lending-service was not asked to create any copies")
    public void lendingNotAsked() {
        assertTrue(FakeLending.INSTANCE.requests().stream().noneMatch(r -> r.equals("POST /api/inventory")),
                "lending-service got " + FakeLending.INSTANCE.requests());
    }

    @Then("the book shows {int} copies, {int} on the shelf")
    public void bookShows(int total, int available) {
        assertCopies(api.lastJson(), total, available);
    }

    @Then("the list shows {string} with {int} of {int} copies on the shelf")
    public void listShows(String title, int available, int total) {
        for (JsonNode book : api.lastJson().get("content")) {
            if (book.get("title").asString().equals(title)) {
                assertCopies(book, total, available);
                return;
            }
        }
        throw new AssertionError("\"" + title + "\" is not in the list: " + api.lastBody());
    }

    private static void assertCopies(JsonNode book, int total, int available) {
        assertEquals(total, book.get("totalCopies").asInt(), "total copies of " + book);
        assertEquals(available, book.get("availableCopies").asInt(), "copies on the shelf of " + book);
    }

    /** A create or update body, using the author and genre of the Background. */
    private String bookJson(String title, String isbn, boolean withGenre, int copies) {
        long authorId = state.authors().values().iterator().next();
        String genreIds = withGenre ? String.valueOf(state.genres().values().iterator().next()) : "";
        return "{\"title\":\"" + title + "\",\"isbn\":\"" + isbn + "\",\"publishedYear\":1965,"
                + "\"authorId\":" + authorId + ",\"genreIds\":[" + genreIds + "],\"totalCopies\":" + copies + "}";
    }
}
