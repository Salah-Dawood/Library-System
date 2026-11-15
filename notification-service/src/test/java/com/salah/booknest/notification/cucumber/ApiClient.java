package com.salah.booknest.notification.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Sends HTTP requests to the running notification-service and remembers the last response.
 * A new one is made for every scenario.
 */
@Component
@ScenarioScope
public class ApiClient {

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Value("${local.server.port}")
    private int port;

    private int lastStatus;
    private String lastBody;

    public void send(String method, String path, String token) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            builder.method(method, HttpRequest.BodyPublishers.noBody());
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            lastStatus = response.statusCode();
            lastBody = response.body();
        } catch (Exception e) {
            throw new IllegalStateException("Request " + method + " " + path + " failed", e);
        }
    }

    public int lastStatus() {
        return lastStatus;
    }

    public String lastBody() {
        return lastBody;
    }

    public JsonNode lastJson() {
        return jsonMapper.readTree(lastBody);
    }
}
