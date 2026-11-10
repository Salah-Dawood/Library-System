package com.salah.booknest.user.cucumber;

import com.salah.booknest.user.repository.UserRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Steps every feature uses: a clean database and no emails before each scenario, and checks on the last response.
 */
public class CommonSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SentEmails sentEmails;

    @Before
    public void startClean() {
        userRepository.deleteAll();
        sentEmails.clear();
    }

    @When("someone checks the health of the service")
    public void checkHealth() {
        api.send("GET", "/actuator/health", null, null);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertEquals(status, api.lastStatus(), "unexpected status, body was: " + api.lastBody());
    }

    @Then("the error is {string}")
    public void theErrorIs(String error) {
        String actual = api.lastJson().has("error") ? api.lastJson().get("error").asString() : api.lastJson().get("status").asString();
        assertEquals(error, actual, "unexpected error, body was: " + api.lastBody());
    }

    @Then("the service reports {string}")
    public void theServiceReports(String status) {
        assertEquals(status, api.lastJson().get("status").asString());
    }
}
