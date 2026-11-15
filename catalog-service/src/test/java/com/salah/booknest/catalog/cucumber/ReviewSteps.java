package com.salah.booknest.catalog.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of reviews.feature. Whether a member returned a book is asked to (fake) lending-service.
 */
public class ReviewSteps {

    @Autowired private ApiClient api;
    @Autowired private ScenarioState state;
    @Autowired private Tokens tokens;

    // the review id of each member who reviewed in this scenario
    private final Map<String, Long> reviewIds = new HashMap<>();

    @Given("{string} has returned {string}")
    public void hasReturned(String username, String title) {
        FakeLending.INSTANCE.markReturned(username, state.book(title));
    }

    @Given("{string} has reviewed {string} with {int} out of 10")
    public void hasReviewed(String username, String title, int rating) {
        hasReturned(username, title);
        reviews(username, title, rating, "Good read");
        assertEquals(201, api.lastStatus(), "could not review: " + api.lastBody());
    }

    @When("{string} reviews {string} with {int} out of 10 saying {string}")
    public void reviews(String username, String title, int rating, String comment) {
        api.send("POST", "/api/books/" + state.book(title) + "/reviews", tokens.member(username), review(rating, comment));
        if (api.lastStatus() == 201) {
            reviewIds.put(username, api.lastJson().get("id").asLong());
        }
    }

    @When("{string} reviews {string} with {int} out of 10")
    public void reviewsWithoutComment(String username, String title, int rating) {
        reviews(username, title, rating, "No comment");
    }

    @When("{string} changes the review by {string} to {int} out of 10")
    public void changesReview(String username, String author, int rating) {
        api.send("PUT", "/api/reviews/" + reviewIds.get(author), tokens.member(username), review(rating, "Changed"));
    }

    @When("{string} deletes the review by {string}")
    public void deletesReview(String username, String author) {
        api.send("DELETE", "/api/reviews/" + reviewIds.get(author), tokens.member(username), null);
    }

    @When("the librarian deletes the review by {string}")
    public void librarianDeletesReview(String author) {
        api.send("DELETE", "/api/reviews/" + reviewIds.get(author), tokens.librarian(), null);
    }

    @When("{string} opens the reviews of {string}")
    public void opensReviews(String username, String title) {
        api.send("GET", "/api/books/" + state.book(title) + "/reviews", tokens.member(username), null);
        assertEquals(200, api.lastStatus(), "could not open the reviews: " + api.lastBody());
    }

    @Then("the book has {int} review(s) with an average of {double}")
    public void bookHasReviews(int count, double average) {
        assertEquals(count, api.lastJson().get("reviewCount").asInt(), api.lastBody());
        assertEquals(average, api.lastJson().get("averageRating").asDouble(), api.lastBody());
    }

    @Then("the book has no reviews")
    public void bookHasNoReviews() {
        assertEquals(0, api.lastJson().get("reviewCount").asInt(), api.lastBody());
    }

    @Then("they are offered to write a review")
    public void offeredToReview() {
        assertTrue(api.lastJson().get("canReview").asBoolean(), api.lastBody());
    }

    @Then("they are not offered to write a review")
    public void notOfferedToReview() {
        assertFalse(api.lastJson().get("canReview").asBoolean(), api.lastBody());
    }

    private static String review(int rating, String comment) {
        return "{\"rating\":" + rating + ",\"comment\":\"" + comment + "\"}";
    }
}
