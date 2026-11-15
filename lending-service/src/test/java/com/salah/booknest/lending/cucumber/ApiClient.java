package com.salah.booknest.lending.cucumber;

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
 * Sends HTTP requests to the running lending-service and remembers the last response.
 * It is safe to use from many threads at once (the concurrency scenarios), each call returns its own reply.
 * A new one is made for every scenario.
 */
@Component
@ScenarioScope
public class ApiClient {

    public record Reply(int status, String body) {
    }

    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Value("${local.server.port}")
    private int port;

    private volatile Reply last;

    /** Sends the request, remembers it as the last reply, and returns it. */
    public Reply send(String method, String path, String token, String json) {
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
            Reply reply = new Reply(response.statusCode(), response.body());
            last = reply;
            return reply;
        } catch (Exception e) {
            throw new IllegalStateException("Request " + method + " " + path + " failed", e);
        }
    }

    public Reply last() {
        return last;
    }

    public JsonNode json(Reply reply) {
        return jsonMapper.readTree(reply.body());
    }

    public JsonNode lastJson() {
        return json(last);
    }
}
