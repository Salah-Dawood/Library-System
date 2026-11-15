package com.salah.booknest.catalog.cucumber;

import com.salah.booknest.catalog.repository.AuthorRepository;
import com.salah.booknest.catalog.repository.BookRepository;
import com.salah.booknest.catalog.repository.GenreRepository;
import com.salah.booknest.catalog.repository.ReviewRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Steps every feature uses: a clean catalogue and an empty lending-service before each scenario,
 * the health check and checks on the last response.
 */
public class CommonSteps {

    @Autowired
    private ApiClient api;

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private AuthorRepository authorRepository;
    @Autowired private GenreRepository genreRepository;

    @Before
    public void startClean() {
        reviewRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
        genreRepository.deleteAll();
        FakeLending.INSTANCE.reset();
    }

    @When("someone checks the health of the service")
    public void checkHealth() {
        api.send("GET", "/actuator/health", null, null);
    }

    @When("someone opens the book list without logging in")
    public void openBooksWithoutLogin() {
        api.send("GET", "/api/books", null, null);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertEquals(status, api.lastStatus(), "unexpected status, body was: " + api.lastBody());
    }

    @Then("the error is {string}")
    public void theErrorIs(String error) {
        assertEquals(error, api.lastJson().get("error").asString(), "unexpected error, body was: " + api.lastBody());
    }

    @Then("the service reports {string}")
    public void theServiceReports(String status) {
        assertEquals(status, api.lastJson().get("status").asString());
    }
}
