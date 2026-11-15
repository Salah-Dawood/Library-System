package com.salah.booknest.catalog.cucumber;

import com.salah.booknest.catalog.repository.BookRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of lending-connection.feature: what catalog-service does when lending-service is down or failing.
 */
public class LendingConnectionSteps {

    @Autowired private BookRepository bookRepository;

    @Given("lending-service is down")
    public void lendingIsDown() {
        FakeLending.INSTANCE.setMode(FakeLending.Mode.DOWN);
    }

    @Given("lending-service fails with an internal error")
    public void lendingIsBroken() {
        FakeLending.INSTANCE.setMode(FakeLending.Mode.BROKEN);
    }

    @When("lending-service is back")
    public void lendingIsBack() {
        FakeLending.INSTANCE.setMode(FakeLending.Mode.UP);
    }

    @Then("lending-service was asked with the token of {string}")
    public void lendingAskedBy(String username) {
        assertEquals(username, FakeLending.INSTANCE.lastCaller());
    }

    @Then("the catalogue has no book called {string}")
    public void noBookCalled(String title) {
        assertTrue(bookRepository.findAll().stream().noneMatch(book -> book.getTitle().equals(title)),
                "\"" + title + "\" is still in the catalogue");
    }
}
