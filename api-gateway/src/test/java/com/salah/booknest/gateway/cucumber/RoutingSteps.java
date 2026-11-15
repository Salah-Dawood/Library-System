package com.salah.booknest.gateway.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of routing.feature and health.feature: requests go to the real gateway, and the fake service
 * that answers says what it received.
 */
public class RoutingSteps {

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Value("${local.server.port}")
    private int port;

    private HttpResponse<String> last;

    @Before
    public void allServicesUp() {
        FakeServices.INSTANCE.reset();
    }

    @Given("{string} is down")
    public void serviceIsDown(String service) {
        FakeServices.INSTANCE.setDown(service);
    }

    @When("someone checks the health of the gateway")
    public void checkHealth() {
        send("GET", "/actuator/health", null, null);
    }

    @When("the browser asks for {string}")
    public void browserAsks(String path) {
        send("GET", path, null, null);
    }

    @When("the browser sends {string} {string} with the token {string} and the body {string}")
    public void browserSends(String method, String path, String token, String body) {
        send(method, path, token, body);
    }

    @Then("{string} answers")
    public void serviceAnswers(String service) {
        assertEquals(200, last.statusCode(), "body: " + last.body());
        assertEquals(service, json().get("service").asString(), "body: " + last.body());
    }

    @Then("it received the path {string}")
    public void receivedPath(String path) {
        assertEquals(path, json().get("path").asString());
    }

    @Then("it received the query {string}")
    public void receivedQuery(String query) {
        assertEquals(query, json().get("query").asString());
    }

    @Then("it received the method {string}, the token {string} and the body {string}")
    public void receivedRequest(String method, String token, String body) {
        assertEquals(method, json().get("method").asString());
        assertEquals("Bearer " + token, json().get("authorization").asString());
        assertEquals(body, json().get("body").asString());
    }

    @Then("it was told the caller's address")
    public void toldCallerAddress() {
        JsonNode forwardedFor = json().get("forwardedFor");
        assertTrue(forwardedFor != null && !forwardedFor.isNull() && forwardedFor.asString().contains("127.0.0.1"),
                "X-Forwarded-For was " + forwardedFor);
    }

    @Then("the response status is {int}")
    public void responseStatus(int status) {
        assertEquals(status, last.statusCode(), "body: " + last.body());
    }

    @Then("the response still says it was answered by {string}")
    public void answeredByHeader(String service) {
        assertEquals(service, last.headers().firstValue("X-Answered-By").orElse(null));
    }

    @Then("the gateway answers with a server error")
    public void serverError() {
        assertTrue(last.statusCode() >= 500, "status was " + last.statusCode() + ", body: " + last.body());
    }

    @Then("the gateway reports {string}")
    public void gatewayReports(String status) {
        assertEquals(200, last.statusCode(), "body: " + last.body());
        assertEquals(status, json().get("status").asString());
    }

    private JsonNode json() {
        return jsonMapper.readTree(last.body());
    }

    private void send(String method, String path, String token, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json");
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            last = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException("Request " + method + " " + path + " failed", e);
        }
    }
}
