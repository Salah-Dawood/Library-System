package com.salah.booknest.user.cucumber;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class LoginSteps {

    @Autowired
    private ApiClient api;

    @When("{string} logs in with password {string}")
    public void logsIn(String username, String password) {
        api.send("POST", "/auth/users/login", null, api.json("username", username, "password", password));
        if (api.lastStatus() == 200) {
            api.rememberToken(username, api.lastJson().get("token").asString());
        }
    }

    @Then("the login gives a token for {string} with role {string}")
    public void theLoginGivesAToken(String username, String role) {
        String token = api.lastJson().get("token").asString();
        // the middle part of a JWT is its readable content (claims)
        String claimsJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        JsonNode claims = JsonMapper.builder().build().readTree(claimsJson);
        assertEquals(username, claims.get("sub").asString());
        assertEquals(role, claims.get("role").asString());
        assertEquals(api.lastJson().get("user").get("id").asLong(), claims.get("userId").asLong());
    }

    @Then("no token is given")
    public void noTokenIsGiven() {
        assertFalse(api.lastJson().has("token"), "a token was given: " + api.lastBody());
    }
}
