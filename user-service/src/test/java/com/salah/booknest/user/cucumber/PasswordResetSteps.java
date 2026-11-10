package com.salah.booknest.user.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PasswordResetSteps {

    // the link in the email ends with ?token=<the reset token>
    private static final Pattern RESET_TOKEN = Pattern.compile("token=([A-Za-z0-9._-]+)");

    @Autowired
    private ApiClient api;

    @Autowired
    private SentEmails sentEmails;

    @Autowired
    private LoginSteps loginSteps;

    @When("someone asks for a password reset for {string}")
    public void asksForAReset(String username) {
        api.send("PUT", "/auth/email/forgotpassword", null, api.json("username", username));
    }

    @Given("{string} asked for a password reset")
    public void askedForAReset(String username) {
        asksForAReset(username);
        assertEquals(200, api.lastStatus(), "could not ask for a reset, body was: " + api.lastBody());
    }

    @When("the new password {string} is set with the link from the email")
    public void setWithTheLink(String newPassword) {
        setWithToken(newPassword, tokenFromTheEmail());
    }

    @When("the new password {string} is set with the token {string}")
    public void setWithToken(String newPassword, String token) {
        api.send("PUT", "/auth/users/passwordreset/" + token, null, api.json("newPassword", newPassword));
    }

    @When("the new password {string} is set with the login token of {string}")
    public void setWithTheLoginToken(String newPassword, String username) {
        setWithToken(newPassword, api.tokenOf(username));
    }

    @When("the reset token from the email is used to open the profile of {string}")
    public void useTheResetTokenToLogIn(String username) {
        api.send("GET", "/api/member/profile", tokenFromTheEmail(), null);
    }

    @Then("the answer is {string}")
    public void theAnswerIs(String answer) {
        assertEquals(answer, api.lastBody());
    }

    @Then("a reset email was sent to {string}")
    public void aResetEmailWasSent(String address) {
        assertEquals(1, sentEmails.all().size(), "expected one email");
        SentEmails.Email email = sentEmails.all().get(0);
        assertEquals(address, email.to());
        assertTrue(email.subject().contains("Password Reset"), "unexpected subject: " + email.subject());
    }

    @Then("no email was sent")
    public void noEmailWasSent() {
        assertTrue(sentEmails.all().isEmpty(), "an email was sent: " + sentEmails.all());
    }

    @Then("{string} cannot log in with password {string}")
    public void cannotLogIn(String username, String password) {
        loginSteps.logsIn(username, password);
        assertEquals(401, api.lastStatus(), username + " could still log in, body was: " + api.lastBody());
    }

    private String tokenFromTheEmail() {
        assertTrue(!sentEmails.all().isEmpty(), "no reset email was sent");
        Matcher matcher = RESET_TOKEN.matcher(sentEmails.all().get(0).html());
        assertTrue(matcher.find(), "no reset link in the email");
        return matcher.group(1);
    }
}
