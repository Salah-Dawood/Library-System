package com.salah.booknest.user.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

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

    // the token of every user who logged in during this scenario
    private final Map<String, String> tokens = new HashMap<>();

    public void rememberToken(String username, String token) {
        tokens.put(username, token);
    }

    public String tokenOf(String username) {
        String token = tokens.get(username);
        if (token == null) {
            throw new IllegalStateException(username + " has not logged in in this scenario");
        }
        return token;
    }

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

    /**
     * Sends a form like a browser does when it uploads a file (multipart/form-data).
     * fileName may be null to send only the text fields.
     */
    public void sendForm(String method, String path, String token, Map<String, String> fields,
                         String fileName, String fileType, byte[] fileBytes) {
        String boundary = "----BookNestTest" + System.nanoTime();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        fields.forEach((name, value) -> write(body, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n"));
        if (fileName != null) {
            write(body, "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: " + fileType + "\r\n\r\n");
            body.writeBytes(fileBytes);
            write(body, "\r\n");
        }
        write(body, "--" + boundary + "--\r\n");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Authorization", "Bearer " + token)
                    .method(method, HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            lastStatus = response.statusCode();
            lastBody = response.body();
        } catch (Exception e) {
            throw new IllegalStateException("Form " + method + " " + path + " failed", e);
        }
    }

    private static void write(ByteArrayOutputStream body, String text) {
        body.writeBytes(text.getBytes(StandardCharsets.UTF_8));
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
