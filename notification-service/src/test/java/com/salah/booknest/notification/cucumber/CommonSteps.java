package com.salah.booknest.notification.cucumber;

import com.salah.booknest.notification.repository.ProcessedEventRepository;
import com.salah.booknest.notification.repository.RecipientRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Steps every feature uses: no recipients, handled events or emails before each scenario,
 * the health check and checks on the last response.
 */
public class CommonSteps {

    @Autowired private ApiClient api;
    @Autowired private RecipientRepository recipientRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;
    @Autowired private SentEmails sentEmails;

    @Before
    public void startClean() {
        recipientRepository.deleteAll();
        processedEventRepository.deleteAll();
        sentEmails.clear();
    }

    @When("someone checks the health of the service")
    public void checkHealth() {
        api.send("GET", "/actuator/health", null);
    }

    @When("someone opens the live notifications without logging in")
    public void openStreamWithoutLogin() {
        api.send("GET", "/api/notifications/stream", null);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertEquals(status, api.lastStatus(), "unexpected status, body was: " + api.lastBody());
    }

    @Then("the service reports {string}")
    public void theServiceReports(String status) {
        assertEquals(status, api.lastJson().get("status").asString());
    }
}
