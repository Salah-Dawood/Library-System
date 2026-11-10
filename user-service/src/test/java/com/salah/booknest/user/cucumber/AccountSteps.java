package com.salah.booknest.user.cucumber;

import com.salah.booknest.user.model.User;
import com.salah.booknest.user.repository.UserRepository;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Creates the accounts a scenario starts with. Accounts are registered through the real API;
 * things a member cannot do through the API themselves (being verified, deactivated or a librarian)
 * are set directly in the database.
 */
public class AccountSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private RegistrationSteps registrationSteps;

    @Autowired
    private UserRepository userRepository;

    @Given("a member {string} with password {string} who has not verified their email")
    public void anUnverifiedMember(String username, String password) {
        Map<String, String> details = RegistrationSteps.validRegistration(username, username + "@example.com");
        details.put("password", password);
        registrationSteps.register(details);
        assertEquals(201, api.lastStatus(), "could not register " + username + ", body was: " + api.lastBody());
    }

    @Given("a verified member {string} with password {string}")
    public void aVerifiedMember(String username, String password) {
        anUnverifiedMember(username, password);
        User user = findUser(username);
        user.setIsVerified(true);
        userRepository.save(user);
    }

    @Given("a librarian {string} with password {string}")
    public void aLibrarian(String username, String password) {
        aVerifiedMember(username, password);
        User user = findUser(username);
        user.setRole("librarian");
        userRepository.save(user);
    }

    @Given("the account {string} is deactivated")
    public void theAccountIsDeactivated(String username) {
        User user = findUser(username);
        user.setIsActive(false);
        userRepository.save(user);
    }

    User findUser(String username) {
        return userRepository.findUserByUsername(username).orElseThrow();
    }
}
