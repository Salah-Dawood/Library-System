package com.salah.booknest.user.cucumber;

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
 * Sends HTTP requests to the running user-service and remembers the last response,
 * so a "When" step can send a request and the "Then" steps can check it.
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

    public void send(String method, String path, String token, String json) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json");
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            builder.method(method, json == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json));
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

    /** The last response body as JSON. */
    public JsonNode lastJson() {
        return jsonMapper.readTree(lastBody);
    }

    /** Builds a JSON body from name/value pairs, for example json("username", "alice", "password", "x"). */
    public String json(Object... namesAndValues) {
        var node = jsonMapper.createObjectNode();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            Object value = namesAndValues[i + 1];
            if (value instanceof Integer number) {
                node.put((String) namesAndValues[i], number);
            } else {
                node.put((String) namesAndValues[i], value == null ? null : value.toString());
            }
        }
        return node.toString();
    }
}
