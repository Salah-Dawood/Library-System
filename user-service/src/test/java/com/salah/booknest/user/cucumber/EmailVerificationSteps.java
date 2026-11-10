package com.salah.booknest.user.cucumber;

import com.salah.booknest.user.model.User;
import com.salah.booknest.user.repository.UserRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The email is never really sent in the tests, so "the code from the email" is read from the database.
 */
public class EmailVerificationSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginSteps loginSteps;

    // the code used in this scenario, so "the same code again" can send it once more
    private Integer usedCode;

    @When("{string} asks for a verification code")
    public void asksForACode(String username) {
        api.send("PUT", "/auth/email/getverification/" + username, null, null);
    }

    @Given("{string} asked for a verification code")
    public void askedForACode(String username) {
        asksForACode(username);
        assertEquals(200, api.lastStatus(), "could not get a code, body was: " + api.lastBody());
    }

    @When("{string} enters the code from the email")
    public void entersTheCode(String username) {
        usedCode = user(username).getEmailVerificationCode();
        enterCode(username, usedCode);
    }

    @Given("{string} entered the code from the email")
    public void enteredTheCode(String username) {
        entersTheCode(username);
        assertEquals(200, api.lastStatus(), "verification failed, body was: " + api.lastBody());
    }

    @When("{string} enters the same code again")
    public void entersTheSameCodeAgain(String username) {
        enterCode(username, usedCode);
    }

    @When("{string} enters a wrong code")
    public void entersAWrongCode(String username) {
        int right = user(username).getEmailVerificationCode();
        enterCode(username, right == 9999 ? 1000 : right + 1);
    }

    @Given("the code of {string} has expired")
    public void theCodeHasExpired(String username) {
        User user = user(username);
        user.setEmailVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));
        userRepository.save(user);
    }

    @Then("a 4 digit code is waiting for {string} for {int} minutes")
    public void aCodeIsWaiting(String username, int minutes) {
        User user = user(username);
        Integer code = user.getEmailVerificationCode();
        assertNotNull(code, "no code was stored");
        assertTrue(code >= 1000 && code <= 9999, "the code is not 4 digits: " + code);
        long minutesLeft = Duration.between(LocalDateTime.now(), user.getEmailVerificationCodeExpiresAt()).toMinutes();
        assertTrue(minutesLeft >= minutes - 1 && minutesLeft <= minutes, "the code is valid for " + minutesLeft + " minutes");
    }

    @Then("{string} is verified")
    public void isVerified(String username) {
        assertTrue(user(username).getIsVerified());
    }

    @Then("{string} is still not verified")
    public void isStillNotVerified(String username) {
        assertFalse(user(username).getIsVerified());
    }

    @Then("{string} can log in with password {string}")
    public void canLogIn(String username, String password) {
        loginSteps.logsIn(username, password);
        assertEquals(200, api.lastStatus(), username + " could not log in, body was: " + api.lastBody());
    }

    private void enterCode(String username, Integer code) {
        api.send("PUT", "/auth/email/verify/" + username + "?code=" + code, null, null);
    }

    private User user(String username) {
        return userRepository.findUserByUsername(username).orElseThrow();
    }
}
