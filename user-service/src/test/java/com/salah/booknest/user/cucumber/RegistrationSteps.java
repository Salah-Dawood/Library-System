package com.salah.booknest.user.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegistrationSteps {

    @Autowired
    private ApiClient api;

    /** A registration that passes every rule; scenarios change one field at a time. */
    static Map<String, String> validRegistration(String username, String email) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("username", username);
        details.put("emailAddress", email);
        details.put("password", "Passw0rd!");
        details.put("firstName", "Test");
        details.put("lastName", "User");
        details.put("age", "25");
        return details;
    }

    void register(Map<String, String> details) {
        Object[] namesAndValues = new Object[details.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> entry : details.entrySet()) {
            namesAndValues[i++] = entry.getKey();
            // age is a number in the JSON, an empty cell means the field is left empty
            namesAndValues[i++] = "age".equals(entry.getKey()) && !entry.getValue().isBlank()
                    ? Integer.valueOf(entry.getValue())
                    : entry.getValue();
        }
        api.send("POST", "/auth/users/register", null, api.json(namesAndValues));
    }

    @Given("an account {string} with email {string} already exists")
    public void anAccountAlreadyExists(String username, String email) {
        register(validRegistration(username, email));
        assertEquals(201, api.lastStatus(), "could not create the account, body was: " + api.lastBody());
    }

    @When("someone registers with:")
    public void someoneRegistersWith(Map<String, String> details) {
        register(details);
    }

    @When("someone registers as {string} with email {string}")
    public void someoneRegistersAs(String username, String email) {
        register(validRegistration(username, email));
    }

    @When("someone registers with {string} set to {string}")
    public void someoneRegistersWithOneField(String field, String value) {
        Map<String, String> details = validRegistration("newuser", "newuser@example.com");
        details.put(field, value);
        register(details);
    }

    @Then("the new account {string} is a {string} that is not verified yet")
    public void theNewAccountIs(String username, String role) {
        JsonNode account = api.lastJson();
        assertEquals(username, account.get("username").asString());
        assertEquals(role, account.get("role").asString());
        assertFalse(account.get("isVerified").asBoolean());
        assertTrue(account.get("isActive").asBoolean());
    }

    @Then("the response does not show the password")
    public void theResponseDoesNotShowThePassword() {
        assertFalse(api.lastBody().contains("password"), "the password is in the response: " + api.lastBody());
        assertFalse(api.lastBody().contains("Passw0rd!"), "the password is in the response: " + api.lastBody());
    }

    @Then("the field {string} is rejected")
    public void theFieldIsRejected(String field) {
        JsonNode fieldErrors = api.lastJson().get("fieldErrors");
        assertTrue(fieldErrors != null && fieldErrors.has(field), "no error for " + field + ", body was: " + api.lastBody());
    }
}
