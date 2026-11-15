package com.salah.booknest.notification.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Open live-notification streams (Server-Sent Events), one per user, like the browser keeps open.
 * Every line the service pushes is kept, so the steps can check which events arrived where.
 * A new one is made for every scenario.
 */
@Component
@ScenarioScope
public class LiveStreams {

    private final HttpClient client = HttpClient.newHttpClient();
    private final Map<String, List<String>> lines = new ConcurrentHashMap<>();
    private final List<Stream<String>> open = new CopyOnWriteArrayList<>();

    @Value("${local.server.port}")
    private int port;

    public void open(String username, String token) {
        List<String> received = lines.computeIfAbsent(username, key -> new CopyOnWriteArrayList<>());
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/notifications/stream"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "text/event-stream")
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofLines()).thenAccept(response -> {
            Stream<String> stream = response.body();
            open.add(stream);
            Thread reader = new Thread(() -> {
                try {
                    stream.forEach(received::add);
                } catch (RuntimeException closed) {
                    // the stream was closed at the end of the scenario
                }
            }, "sse-" + username);
            reader.setDaemon(true);
            reader.start();
        });
    }

    /** True if the user's stream received an event with this name (an "event:NAME" line). */
    public boolean received(String username, String eventName) {
        return lines.getOrDefault(username, List.of()).stream()
                .anyMatch(line -> line.replace(" ", "").equals("event:" + eventName));
    }

    public List<String> lines(String username) {
        return lines.getOrDefault(username, List.of());
    }

    public void closeAll() {
        open.forEach(Stream::close);
        open.clear();
    }
}
