package com.salah.booknest.lending.cucumber;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Steps every feature uses: the health check and checks on the last response.
 */
public class CommonSteps {

    @Autowired private ApiClient api;

    @When("someone checks the health of the service")
    public void checkHealth() {
        api.send("GET", "/actuator/health", null, null);
    }

    @When("someone opens their loans without logging in")
    public void openLoansWithoutLogin() {
        api.send("GET", "/api/loans/my-loans", null, null);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertEquals(status, api.last().status(), "unexpected status, body was: " + api.last().body());
    }

    @Then("the error is {string}")
    public void theErrorIs(String error) {
        assertEquals(error, api.lastJson().get("error").asString(), "unexpected error, body was: " + api.last().body());
    }

    @Then("the service reports {string}")
    public void theServiceReports(String status) {
        assertEquals(status, api.lastJson().get("status").asString());
    }
}
