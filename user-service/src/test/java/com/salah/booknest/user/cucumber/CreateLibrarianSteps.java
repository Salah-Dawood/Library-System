package com.salah.booknest.user.cucumber;

import com.salah.booknest.user.model.User;
import com.salah.booknest.user.repository.UserRepository;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of create-librarian.feature: a librarian creates another librarian account.
 */
public class CreateLibrarianSteps {

    @Autowired private ApiClient api;
    @Autowired private UserRepository userRepository;

    @When("{string} creates the librarian {string} with email {string} and password {string}")
    public void createsLibrarian(String creator, String username, String email, String password) {
        api.send("POST", "/api/librarian/users", api.tokenOf(creator), details(username, email, password, "30"));
    }

    @When("someone without a token creates the librarian {string} with email {string}")
    public void createsWithoutToken(String username, String email) {
        api.send("POST", "/api/librarian/users", null, details(username, email, "Start123!", "30"));
    }

    @When("{string} creates a librarian with {string} set to {string}")
    public void createsWithField(String creator, String field, String value) {
        String username = field.equals("username") ? value : "sara";
        String email = field.equals("emailAddress") ? value : "sara@example.com";
        String password = field.equals("password") ? value : "Start123!";
        String age = field.equals("age") ? value : "30";
        api.send("POST", "/api/librarian/users", api.tokenOf(creator), details(username, email, password, age));
    }

    @Then("the new account {string} is a verified librarian")
    public void verifiedLibrarian(String username) {
        User user = account(username).orElseThrow(() -> new AssertionError("no account " + username));
        assertEquals("librarian", user.getRole());
        assertTrue(user.getIsVerified(), username + " is not verified");
        assertTrue(user.getIsActive(), username + " is not active");
    }

    @Then("there is no account {string}")
    public void noAccount(String username) {
        assertTrue(account(username).isEmpty(), "the account " + username + " was created");
    }

    private Optional<User> account(String username) {
        return userRepository.findAll().stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    private String details(String username, String email, String password, String age) {
        return api.json("username", username, "emailAddress", email, "password", password,
                "firstName", "Sara", "lastName", "Ali", "age", age.isBlank() ? null : Integer.valueOf(age));
    }
}
