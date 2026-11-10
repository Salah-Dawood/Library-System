package com.salah.booknest.user.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserAdministrationSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private AccountSteps accountSteps;

    @Autowired
    private LoginSteps loginSteps;

    @When("{string} lists the users")
    public void listsTheUsers(String username) {
        api.send("GET", "/api/librarian/users?size=100", api.tokenOf(username), null);
    }

    @When("{string} opens the account of {string}")
    public void opensTheAccountOf(String librarian, String username) {
        opensTheAccountWithId(librarian, idOf(username));
    }

    @When("{string} opens the account with id {long}")
    public void opensTheAccountWithId(String librarian, long id) {
        api.send("GET", "/api/librarian/users/" + id, api.tokenOf(librarian), null);
    }

    @When("{string} deactivates {string}")
    public void deactivates(String librarian, String username) {
        api.send("PUT", "/api/librarian/users/deactivate/" + idOf(username), api.tokenOf(librarian), null);
    }

    @Given("{string} deactivated {string}")
    public void deactivated(String librarian, String username) {
        deactivates(librarian, username);
        assertEquals(200, api.lastStatus(), "could not deactivate " + username + ", body was: " + api.lastBody());
    }

    @When("{string} activates {string}")
    public void activates(String librarian, String username) {
        api.send("PUT", "/api/librarian/users/activate/" + idOf(username), api.tokenOf(librarian), null);
    }

    @Then("the list contains {string} and {string}")
    public void theListContains(String first, String second) {
        List<String> usernames = new ArrayList<>();
        for (JsonNode user : api.lastJson().get("content")) {
            usernames.add(user.get("username").asString());
        }
        assertTrue(usernames.contains(first) && usernames.contains(second), "the list was " + usernames);
    }

    @Then("the account shows username {string} with email {string}")
    public void theAccountShows(String username, String email) {
        assertEquals(username, api.lastJson().get("username").asString());
        assertEquals(email, api.lastJson().get("emailAddress").asString());
    }

    @Then("the account does not show the password")
    public void theAccountDoesNotShowThePassword() {
        assertFalse(api.lastJson().has("password"), "the password is in the response: " + api.lastBody());
    }

    @Then("{string} is shown as not active")
    public void isShownAsNotActive(String username) {
        assertEquals(username, api.lastJson().get("username").asString());
        assertFalse(api.lastJson().get("isActive").asBoolean());
    }

    @Then("{string} cannot log in because the account is deactivated")
    public void cannotLogInBecauseDeactivated(String username) {
        loginSteps.logsIn(username, "Passw0rd!");
        assertEquals(403, api.lastStatus());
        assertEquals("ACCOUNT_DEACTIVATED", api.lastJson().get("error").asString());
    }

    private long idOf(String username) {
        return accountSteps.findUser(username).getId();
    }
}
