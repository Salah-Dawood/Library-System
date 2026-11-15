package com.salah.booknest.lending.cucumber;

import com.salah.booknest.lending.cucumber.ApiClient.Reply;
import com.salah.booknest.lending.model.Fine;
import com.salah.booknest.lending.model.FineStatus;
import com.salah.booknest.lending.model.LoanStatus;
import com.salah.booknest.lending.model.WaitlistEntry;
import com.salah.booknest.lending.repository.FineRepository;
import com.salah.booknest.lending.repository.LoanRepository;
import com.salah.booknest.lending.service.WaitlistService;
import com.salah.booknest.lending.support.ConcurrentRunner;
import com.salah.booknest.lending.support.ConcurrentRunner.Outcome;
import com.salah.booknest.lending.support.TestBook;
import com.salah.booknest.lending.support.TestData;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of concurrency.feature. Every "at the same moment" step releases all its requests at once
 * (ConcurrentRunner) against the real REST API, so the database locks are what decide the winner.
 */
public class ConcurrencySteps {

    @Autowired private ApiClient api;
    @Autowired private LendingState state;
    @Autowired private TestData data;
    @Autowired private FineRepository fineRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private WaitlistService waitlistService;

    private List<Reply> replies = new ArrayList<>();
    private Reply claimReply;
    private Fine fine;
    private long seconds;

    // ---------- setup ----------

    @Given("a copy of {string} is held for {string}")
    public void copyHeldFor(String title, String member) {
        held(title, member, 60);
    }

    @Given("a copy of {string} was held for {string} but the hold has run out")
    public void copyHeldRanOut(String title, String member) {
        held(title, member, -5);
    }

    private void held(String title, String member, int minutesLeft) {
        TestBook book = state.book(title);
        WaitlistEntry entry = data.newHoldEntry(state.member(member), book, minutesLeft);
        data.setAvailable(book, 0);
        state.rememberEntry(member, title, entry.getId());
    }

    @Given("{string} has an unpaid fine")
    public void unpaidFine(String member) {
        fine = data.newFine(data.newLoan(state.member(member), data.newBook(1)));
    }

    // ---------- at the same moment ----------

    @When("{int} different members try to borrow {string} at the same moment")
    public void differentMembersBorrow(int count, String title) {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String token = state.token("member " + i);
            long bookId = state.book(title).id();
            tasks.add(() -> borrow(token, bookId));
        }
        runTogether(tasks);
    }

    @When("{string} tries to borrow {string} {int} times at the same moment")
    public void sameMemberBorrows(String member, String title, int count) {
        String token = state.token(member);
        long bookId = state.book(title).id();
        runTogether(repeat(count, () -> borrow(token, bookId)));
    }

    @When("{string} tries to borrow {int} different books at the same moment")
    public void borrowsDifferentBooks(String member, int count) {
        String token = state.token(member);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String title = "Book " + i;
            state.addBook(title, 2);
            long bookId = state.book(title).id();
            tasks.add(() -> borrow(token, bookId));
        }
        runTogether(tasks);
    }

    @When("{string} returns {string} while the librarian cancels it, at the same moment")
    public void returnAndCancel(String member, String title) {
        long loanId = state.loan(member, title);
        String memberToken = state.token(member);
        String librarianToken = state.token("the librarian");
        runTogether(List.of(
                () -> http("PUT", "/api/loans/" + loanId + "/return", memberToken, null),
                () -> http("PUT", "/api/loans/" + loanId + "/cancel", librarianToken, null)));
    }

    @When("{string} returns {string} {int} times at the same moment")
    public void returnsManyTimes(String member, String title, int count) {
        long loanId = state.loan(member, title);
        String token = state.token(member);
        runTogether(repeat(count, () -> http("PUT", "/api/loans/" + loanId + "/return", token, null)));
    }

    @When("two librarians review the return of {string} by {string} at the same moment")
    public void twoLibrariansReview(String title, String member) {
        long loanId = state.loan(member, title);
        String first = data.token(data.newLibrarian());
        String second = data.token(data.newLibrarian());
        runTogether(List.of(
                () -> http("PUT", "/api/loans/" + loanId + "/review-return", first, "{\"damaged\":false}"),
                () -> http("PUT", "/api/loans/" + loanId + "/review-return", second, "{\"damaged\":false}")));
    }

    @When("{string} pays that fine {int} times at the same moment")
    public void paysFineManyTimes(String member, int count) {
        String token = state.token(member);
        long fineId = fine.getId();
        runTogether(repeat(count, () -> http("PUT", "/api/fines/" + fineId + "/pay", token, null)));
    }

    @When("{int} different members join the waitlist of {string} at the same moment")
    public void differentMembersJoin(int count, String title) {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String token = state.token("member " + i);
            long bookId = state.book(title).id();
            tasks.add(() -> join(token, bookId));
        }
        runTogether(tasks);
    }

    @When("{string} joins the waitlist of {string} {int} times at the same moment")
    public void sameMemberJoins(String member, String title, int count) {
        String token = state.token(member);
        long bookId = state.book(title).id();
        runTogether(repeat(count, () -> join(token, bookId)));
    }

    @When("{int} walk-in members try to borrow {string} while {string} claims the held copy, at the same moment")
    public void walkInsAndClaim(int count, String title, String member) {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String token = state.token("walk-in " + i);
            long bookId = state.book(title).id();
            tasks.add(() -> borrow(token, bookId));
        }
        String memberToken = state.token(member);
        long entryId = state.entry(member, title);
        tasks.add(() -> {
            claimReply = http("POST", "/api/waitlist/" + entryId + "/claim?days=7", memberToken, null);
            return claimReply;
        });
        runTogether(tasks);
    }

    @When("{string} claims the held copy of {string} {int} times at the same moment")
    public void claimsManyTimes(String member, String title, int count) {
        String token = state.token(member);
        long entryId = state.entry(member, title);
        runTogether(repeat(count, () -> http("POST", "/api/waitlist/" + entryId + "/claim?days=7", token, null)));
    }

    @When("the expiry of the hold of {string} on {string} runs {int} times at the same moment")
    public void expiryRunsManyTimes(String member, String title, int count) {
        long entryId = state.entry(member, title);
        // a run that finds the hold already gone may be refused; that is fine here
        runTogether(repeat(count, () -> waitlistService.expireHold(entryId)), true);
    }

    @When("{string} gives up the held copy of {string} while the hold expires, at the same moment")
    public void leaveAndExpire(String member, String title) {
        long entryId = state.entry(member, title);
        String token = state.token(member);
        runTogether(List.of(
                () -> http("DELETE", "/api/waitlist/" + entryId, token, null),
                () -> waitlistService.expireHold(entryId)), true);
    }

    /**
     * Every member borrows the three books and queues for two of them, each member in a different order,
     * all at once. If two requests ever locked the same rows in opposite orders they would deadlock.
     */
    @When("{int} members borrow {string}, {string} and {string} and queue for them, in different orders, at the same moment")
    public void mixedTraffic(int members, String a, String b, String c) {
        List<String> titles = List.of(a, b, c);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int m = 0; m < members; m++) {
            String token = state.token("member " + m);
            for (int i = 0; i < titles.size(); i++) {
                long bookId = state.book(titles.get((m + i) % titles.size())).id();
                tasks.add(() -> borrow(token, bookId));
                if (i < 2) {
                    tasks.add(() -> join(token, bookId));
                }
            }
        }
        runTogether(tasks);
    }

    // ---------- checks ----------

    @Then("exactly {int} of them succeeded")
    public void exactlySucceeded(int count) {
        long succeeded = replies.stream().filter(r -> r.status() >= 200 && r.status() < 300).count();
        assertEquals(count, succeeded, "replies: " + replies);
    }

    @Then("{int} of them were refused with {string}")
    public void refusedWith(int count, String code) {
        long refused = replies.stream().filter(r -> r.status() == 409 && r.body().contains("\"" + code + "\"")).count();
        assertEquals(count, refused, "replies: " + replies);
    }

    @Then("exactly 1 of them expired the hold")
    public void expiredOnce() {
        assertTrue(expiredByThisRun <= 1, expiredByThisRun + " runs expired the same hold");
    }

    @Then("{int} member(s) has/have {string} on loan")
    public void membersHaveOnLoan(int count, String title) {
        assertEquals(count, data.countLoans(state.book(title), LoanStatus.BORROWED));
    }

    @Then("everyone in line got a different place, from 1 to {int}")
    public void differentPlaces(int count) {
        Set<Integer> places = new HashSet<>();
        for (Reply reply : replies) {
            assertEquals(201, reply.status(), reply.body());
            places.add(api.json(reply).get("position").asInt());
        }
        assertEquals(count, places.size(), "places: " + places);
        assertTrue(places.contains(1) && places.contains(count), "places: " + places);
    }

    @Then("the held copy went to {string}")
    public void heldCopyWentTo(String member) {
        assertEquals(201, claimReply.status(), "the claim of " + member + " failed: " + claimReply.body());
    }

    @Then("the fine is paid")
    public void finePaid() {
        assertEquals(FineStatus.PAID, fineRepository.findById(fine.getId()).orElseThrow().getStatus());
    }

    @Then("no request failed with a server error")
    public void noServerError() {
        assertTrue(replies.stream().noneMatch(r -> r.status() >= 500), "replies: " + replies);
    }

    @Then("every request was answered within {int} seconds")
    public void answeredWithin(int limit) {
        assertTrue(seconds < limit, "the requests took " + seconds + " s");
    }

    @Then("nobody has more than 5 books on loan")
    public void loanLimitHeld() {
        for (int m = 0; m < 50; m++) {
            if (!state.hasMember("member " + m)) {
                break;
            }
            long id = state.member("member " + m).id();
            long active = loanRepository.countByUserIdAndStatusIn(id, List.of(LoanStatus.BORROWED, LoanStatus.PENDING_REVIEW));
            assertTrue(active <= 5, "member " + m + " has " + active + " books on loan");
        }
    }

    // ---------- helpers ----------

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    /** A plain HTTP call that any thread may make. */
    private Reply http(String method, String path, String token, String json) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json");
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            builder.method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body());
        } catch (Exception e) {
            throw new IllegalStateException("Request " + method + " " + path + " failed", e);
        }
    }

    private long expiredByThisRun;

    private Reply borrow(String token, long bookId) {
        return http("POST", "/api/loans", token, "{\"bookId\":" + bookId + ",\"duration\":7}");
    }

    private Reply join(String token, long bookId) {
        return http("POST", "/api/waitlist", token, "{\"bookId\":" + bookId + "}");
    }

    private static List<Callable<Object>> repeat(int count, Callable<Object> task) {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(task);
        }
        return tasks;
    }

    private void runTogether(List<Callable<Object>> tasks) {
        runTogether(tasks, false);
    }

    /**
     * Runs the tasks together; HTTP replies are kept for the checks, the expiry results are counted.
     * The scenario's own helpers (ApiClient, LendingState) belong to the scenario's thread, so the tasks
     * only use values looked up before and the plain {@link #http} sender.
     */
    private void runTogether(List<Callable<Object>> tasks, boolean allowRefusedRuns) {
        long started = System.currentTimeMillis();
        List<Outcome> outcomes;
        try {
            outcomes = ConcurrentRunner.runTogether(tasks);
        } catch (Exception e) {
            throw new AssertionError("the requests did not finish (a deadlock?)", e);
        }
        seconds = (System.currentTimeMillis() - started) / 1000;
        replies = new ArrayList<>();
        expiredByThisRun = 0;
        for (Outcome outcome : outcomes) {
            if (outcome.error() != null) {
                if (allowRefusedRuns) {
                    continue;
                }
                throw new AssertionError("a request failed instead of getting an answer", outcome.error());
            }
            if (outcome.value() instanceof Reply reply) {
                replies.add(reply);
            } else if (Boolean.TRUE.equals(outcome.value())) {
                expiredByThisRun++;
            }
        }
    }
}
