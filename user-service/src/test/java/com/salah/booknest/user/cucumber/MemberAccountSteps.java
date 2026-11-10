package com.salah.booknest.user.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MemberAccountSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private LoginSteps loginSteps;

    // a picture saved during the scenario, removed again afterwards
    private Path savedPicture;

    @After
    public void removeSavedPicture() throws Exception {
        if (savedPicture != null) {
            Files.deleteIfExists(savedPicture);
        }
    }

    @Given("{string} is logged in with password {string}")
    public void isLoggedIn(String username, String password) {
        loginSteps.logsIn(username, password);
        assertEquals(200, api.lastStatus(), username + " could not log in, body was: " + api.lastBody());
    }

    @When("{string} opens their profile")
    public void opensTheirProfile(String username) {
        api.send("GET", "/api/member/profile", api.tokenOf(username), null);
    }

    @When("someone without a token opens a profile")
    public void opensAProfileWithoutToken() {
        api.send("GET", "/api/member/profile", null, null);
    }

    @When("{string} updates their profile to first name {string}, last name {string}, age {int} and bio {string}")
    public void updatesTheirProfile(String username, String firstName, String lastName, int age, String bio) {
        api.sendForm("PUT", "/api/member/update/profile", api.tokenOf(username),
                profileFields(firstName, lastName, age, bio), null, null, null);
    }

    @When("{string} uploads a real PNG picture")
    public void uploadsAPicture(String username) throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", png);
        api.sendForm("PUT", "/api/member/update/profile", api.tokenOf(username),
                profileFields("Test", "User", 25, ""), "me.png", "image/png", png.toByteArray());
    }

    @When("{string} uploads a text file named {string}")
    public void uploadsATextFile(String username, String fileName) {
        api.sendForm("PUT", "/api/member/update/profile", api.tokenOf(username),
                profileFields("Test", "User", 25, ""), fileName, "image/png",
                "this is not a picture".getBytes(StandardCharsets.UTF_8));
    }

    @When("{string} changes their password from {string} to {string}")
    public void changesTheirPassword(String username, String currentPassword, String newPassword) {
        api.send("PUT", "/api/member/change-password", api.tokenOf(username),
                api.json("currentPassword", currentPassword, "newPassword", newPassword));
    }

    @Then("the profile shows first name {string}, last name {string} and age {int}")
    public void theProfileShows(String firstName, String lastName, int age) {
        JsonNode profile = api.lastJson();
        assertEquals(firstName, profile.get("firstName").asString());
        assertEquals(lastName, profile.get("lastName").asString());
        assertEquals(age, profile.get("age").asInt());
    }

    @Then("the profile of {string} shows first name {string}, last name {string} and age {int}")
    public void theProfileOfShows(String username, String firstName, String lastName, int age) {
        opensTheirProfile(username);
        theProfileShows(firstName, lastName, age);
    }

    @Then("the profile of {string} shows bio {string}")
    public void theProfileOfShowsBio(String username, String bio) {
        opensTheirProfile(username);
        assertEquals(bio, api.lastJson().get("bio").asString());
    }

    @Then("the profile picture of {string} can be downloaded")
    public void theProfilePictureCanBeDownloaded(String username) {
        opensTheirProfile(username);
        String imageUrl = api.lastJson().get("imageUrl").asString().replace('\\', '/');
        savedPicture = Path.of(imageUrl);
        assertTrue(imageUrl.endsWith(".png"), "unexpected picture path: " + imageUrl);
        api.send("GET", "/" + imageUrl, null, null);
        assertEquals(200, api.lastStatus(), "the picture could not be downloaded");
    }

    @Then("{string} has no profile picture")
    public void hasNoProfilePicture(String username) {
        opensTheirProfile(username);
        JsonNode imageUrl = api.lastJson().get("imageUrl");
        assertTrue(imageUrl == null || imageUrl.isNull(), "a picture was saved: " + imageUrl);
    }

    private Map<String, String> profileFields(String firstName, String lastName, int age, String bio) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("firstName", firstName);
        fields.put("lastName", lastName);
        fields.put("age", String.valueOf(age));
        fields.put("bio", bio);
        return fields;
    }
}
