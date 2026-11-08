package com.salah.booknest;

import com.salah.booknest.model.Book;
import com.salah.booknest.model.Fine;
import com.salah.booknest.model.Loan;
import com.salah.booknest.model.User;
import com.salah.booknest.security.JWTUtils;
import com.salah.booknest.security.MyUserDetails;
import com.salah.booknest.support.ConcurrentRunner;
import com.salah.booknest.support.ConcurrentRunner.Outcome;
import com.salah.booknest.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConcurrentRestTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired private JWTUtils jwtUtils;
    @Autowired private TestData data;

    private final HttpClient client = HttpClient.newHttpClient();

    private record Reply(int status, String body) {
    }

    private String token(User user) {
        return jwtUtils.generateJwtToken(new MyUserDetails(user));
    }

    private Reply send(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        builder.method(method, json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json));
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body());
    }

    private long countStatus(List<Outcome> outcomes, int status) {
        return outcomes.stream().filter(o -> ((Reply) o.value()).status() == status).count();
    }

    private boolean allBodiesContain(List<Outcome> outcomes, int status, String text) {
        return outcomes.stream()
                .map(o -> (Reply) o.value())
                .filter(r -> r.status() == status)
                .allMatch(r -> r.body().contains(text));
    }

    @Test
    void tenRequestsForTheLastCopy_oneGets201AndNineGet409() throws Exception {
        Book book = data.newBook(1);
        String json = "{\"bookId\":" + book.getId() + ",\"duration\":7}";
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String token = token(data.newUser());
            tasks.add(() -> send("POST", "/api/loans", token, json));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(1, countStatus(outcomes, 201));
        assertEquals(9, countStatus(outcomes, 409));
        assertTrue(allBodiesContain(outcomes, 409, "NO_COPIES_AVAILABLE"));
        assertEquals(0, data.inventory(book).getAvailableCopies());
        data.assertStockConsistent(book);
    }

    @Test
    void fiveRequestsToPayTheSameFine_oneGets200AndFourGet409() throws Exception {
        User member = data.newUser();
        Loan loan = data.newLoan(member, data.newBook(1));
        Fine fine = data.newFine(loan);
        String token = token(member);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> send("PUT", "/api/fines/" + fine.getId() + "/pay", token, null));
        }

        List<Outcome> outcomes = ConcurrentRunner.runTogether(tasks);

        assertEquals(1, countStatus(outcomes, 200));
        assertEquals(4, countStatus(outcomes, 409));
        assertTrue(allBodiesContain(outcomes, 409, "FINE_ALREADY_PAID"));
    }

    @Test
    void requestWithoutToken_isRejectedWith401() throws Exception {
        Book book = data.newBook(1);

        Reply reply = send("POST", "/api/loans", null, "{\"bookId\":" + book.getId() + ",\"duration\":7}");

        assertEquals(401, reply.status());
        assertEquals(1, data.inventory(book).getAvailableCopies());
    }

    @Test
    void invalidDuration_isRejectedWith400AndTakesNoCopy() throws Exception {
        Book book = data.newBook(1);
        String token = token(data.newUser());

        Reply reply = send("POST", "/api/loans", token, "{\"bookId\":" + book.getId() + ",\"duration\":99}");

        assertEquals(400, reply.status());
        assertEquals(1, data.inventory(book).getAvailableCopies());
    }
}
