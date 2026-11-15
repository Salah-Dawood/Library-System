package com.salah.booknest.gateway.cucumber;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stand-ins for the five services behind the gateway: real HTTP servers that answer with their own name and
 * what they received (method, path, query, body, some headers). A service can be made to look down.
 */
public final class FakeServices {

    // NAMES first: INSTANCE uses it while it is created
    public static final Set<String> NAMES = Set.of("user-service", "catalog-service", "lending-service",
            "notification-service", "frontend");

    public static final FakeServices INSTANCE = new FakeServices();

    private final Map<String, HttpServer> servers;
    private final Set<String> down = ConcurrentHashMap.newKeySet();

    private FakeServices() {
        servers = NAMES.stream().collect(Collectors.toMap(Function.identity(), this::start));
    }

    private HttpServer start(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> answer(name, exchange));
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String url(String name) {
        return "http://localhost:" + servers.get(name).getAddress().getPort();
    }

    public void setDown(String name) {
        down.add(name);
    }

    public void reset() {
        down.clear();
    }

    private void answer(String name, HttpExchange exchange) throws IOException {
        if (down.contains(name)) {
            // the connection is cut without any answer, as when the service has crashed
            exchange.close();
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String json = "{" + Stream.of(
                field("service", name),
                field("method", exchange.getRequestMethod()),
                field("path", path),
                field("query", exchange.getRequestURI().getQuery()),
                field("body", body),
                field("authorization", exchange.getRequestHeaders().getFirst("Authorization")),
                field("forwardedFor", exchange.getRequestHeaders().getFirst("X-Forwarded-For"))
        ).collect(Collectors.joining(",")) + "}";

        // a path ending in /conflict answers 409, to check that errors come back unchanged
        int status = path.endsWith("/conflict") ? 409 : 200;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("X-Answered-By", name);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String field(String key, String value) {
        return "\"" + key + "\":" + (value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
    }
}
