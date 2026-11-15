package com.salah.booknest.catalog.cucumber;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Set;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A small stand-in for lending-service: a real HTTP server with the same inventory API, keeping its own
 * copy counts. catalog-service talks to it over HTTP exactly as it talks to the real one.
 */
public final class FakeLending {

    public static final FakeLending INSTANCE = new FakeLending();

    /** Copies of one book: on the shelf and on loan. */
    private static final class Stock {
        int total;
        int onLoan;

        Stock(int total) {
            this.total = total;
        }

        String json(long bookId) {
            return "{\"bookId\":" + bookId + ",\"totalCopies\":" + total + ",\"availableCopies\":" + (total - onLoan) + "}";
        }
    }

    private final HttpServer server;
    private final Map<Long, Stock> stock = new ConcurrentHashMap<>();
    // "username:bookId" of every member who returned a book
    private final Set<String> returned = ConcurrentHashMap.newKeySet();
    private final List<String> requests = Collections.synchronizedList(new ArrayList<>());

    private FakeLending() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", this::handle);
        server.start();
    }

    public String url() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /** Back to an empty lending-service before every scenario. */
    public void reset() {
        stock.clear();
        returned.clear();
        requests.clear();
    }

    public void markReturned(String username, long bookId) {
        returned.add(username + ":" + bookId);
    }

    public Integer totalCopies(long bookId) {
        Stock s = stock.get(bookId);
        return s == null ? null : s.total;
    }

    public void lend(long bookId, int copies) {
        stock.get(bookId).onLoan += copies;
    }

    /** Every request it received, as "METHOD /path". */
    public List<String> requests() {
        return List.copyOf(requests);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(method + " " + path);

        if (method.equals("GET") && path.equals("/api/inventory")) {
            String ids = exchange.getRequestURI().getQuery().replace("bookIds=", "");
            String list = Pattern.compile(",").splitAsStream(ids)
                    .map(Long::parseLong)
                    .filter(stock::containsKey)
                    .map(id -> stock.get(id).json(id))
                    .collect(Collectors.joining(",", "[", "]"));
            reply(exchange, 200, list);
        } else if (method.equals("POST") && path.equals("/api/inventory")) {
            long bookId = number(body, "bookId");
            if (stock.containsKey(bookId)) {
                reply(exchange, 409, error(409, "RESOURCE_ALREADY_EXISTS", "The book already has stock"));
                return;
            }
            stock.put(bookId, new Stock((int) number(body, "totalCopies")));
            reply(exchange, 201, stock.get(bookId).json(bookId));
        } else if (method.equals("GET") && path.startsWith("/api/loans/returned/")) {
            // like lending-service, it answers for the member whose token catalog-service passed on
            long bookId = Long.parseLong(path.substring("/api/loans/returned/".length()));
            String username = usernameOf(exchange.getRequestHeaders().getFirst("Authorization"));
            reply(exchange, 200, String.valueOf(returned.contains(username + ":" + bookId)));
        } else if (path.startsWith("/api/inventory/")) {
            long bookId = Long.parseLong(path.substring("/api/inventory/".length()));
            Stock s = stock.get(bookId);
            if (s == null) {
                reply(exchange, 404, error(404, "RESOURCE_NOT_FOUND", "No stock for book " + bookId));
            } else if (method.equals("PUT")) {
                int total = (int) number(body, "totalCopies");
                if (total < s.onLoan) {
                    reply(exchange, 400, error(400, "INVALID_REQUEST", "The new total is lower than the copies on loan"));
                    return;
                }
                s.total = total;
                reply(exchange, 200, s.json(bookId));
            } else if (method.equals("DELETE")) {
                if (s.onLoan > 0) {
                    reply(exchange, 409, error(409, "BOOK_ON_LOAN", "A copy of this book is still on loan"));
                    return;
                }
                stock.remove(bookId);
                reply(exchange, 204, null);
            } else {
                reply(exchange, 405, error(405, "METHOD_NOT_ALLOWED", method + " " + path));
            }
        } else {
            reply(exchange, 404, error(404, "RESOURCE_NOT_FOUND", "Unknown path " + path));
        }
    }

    /** The "sub" (username) inside a "Bearer <jwt>" header; the fake only reads it, it does not check the signature. */
    private static String usernameOf(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        String payload = authorization.substring(7).split("\\.")[1];
        String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static long number(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)").matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No " + field + " in " + json);
        }
        return Long.parseLong(matcher.group(1));
    }

    /** The error body lending-service sends (its ApiError). */
    private static String error(int status, String code, String message) {
        return "{\"timestamp\":\"2026-11-15T10:00:00\",\"status\":" + status + ",\"error\":\"" + code
                + "\",\"message\":\"" + message + "\",\"path\":\"/api/inventory\"}";
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }
}
