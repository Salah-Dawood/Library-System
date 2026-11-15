package com.salah.booknest.lending.cucumber;

import com.salah.booknest.lending.cucumber.ApiClient.Reply;
import com.salah.booknest.lending.job.HoldExpiryJob;
import com.salah.booknest.lending.model.WaitlistEntry;
import com.salah.booknest.lending.model.WaitlistStatus;
import com.salah.booknest.lending.repository.WaitlistRepository;
import com.salah.booknest.lending.support.TestBook;
import com.salah.booknest.lending.support.TestData;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of waitlist-and-holds.feature: joining the queue, holds, claiming, leaving and the expiry job.
 */
public class WaitlistSteps {

    @Autowired private ApiClient api;
    @Autowired private LendingState state;
    @Autowired private TestData data;
    @Autowired private WaitlistRepository waitlistRepository;
    @Autowired private HoldExpiryJob holdExpiryJob;

    // the extra holds of the expiry scenarios
    private final List<WaitlistEntry> ranOut = new ArrayList<>();
    private final List<TestBook> ranOutBooks = new ArrayList<>();
    private WaitlistEntry notOverYet;
    private TestBook notOverYetBook;

    // ---------- joining and leaving ----------

    @When("{string} joins the waitlist of {string}")
    public void joins(String member, String title) {
        Reply reply = api.send("POST", "/api/waitlist", state.token(member), "{\"bookId\":" + state.book(title).id() + "}");
        if (reply.status() == 201) {
            state.rememberEntry(member, title, api.json(reply).get("id").asLong());
        }
    }

    @Given("{string} is on the waitlist of {string}")
    public void isOnWaitlist(String member, String title) {
        joins(member, title);
        assertEquals(201, api.last().status(), "could not join the waitlist: " + api.last().body());
    }

    @Given("{string} is already waiting for {int} other books")
    public void alreadyWaiting(String member, int count) {
        for (int i = 0; i < count; i++) {
            TestBook book = data.newBook(1);
            data.setAvailable(book, 0);
            data.newWaitingEntry(state.member(member), book);
        }
    }

    @When("{string} leaves the waitlist of {string}")
    public void leaves(String member, String title) {
        api.send("DELETE", "/api/waitlist/" + entry(member, title), state.token(member), null);
    }

    @When("{string} removes the place of {string} on the waitlist of {string}")
    public void removesSomeoneElses(String member, String owner, String title) {
        api.send("DELETE", "/api/waitlist/" + entry(owner, title), state.token(member), null);
    }

    // ---------- a copy comes back ----------

    @Given("{string} returns {string} in good condition")
    public void returnsInGoodCondition(String member, String title) {
        long loanId = state.loan(member, title);
        Reply returned = api.send("PUT", "/api/loans/" + loanId + "/return", state.token(member), null);
        assertEquals(200, returned.status(), "could not return: " + returned.body());
        Reply reviewed = api.send("PUT", "/api/loans/" + loanId + "/review-return", state.token("the librarian"),
                "{\"damaged\":false}");
        assertEquals(200, reviewed.status(), "could not review: " + reviewed.body());
    }

    @Given("the hold of {string} on {string} has run out")
    public void holdRanOut(String member, String title) {
        WaitlistEntry entry = waitlistRepository.findById(entry(member, title)).orElseThrow();
        entry.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        waitlistRepository.save(entry);
    }

    // ---------- claiming ----------

    @When("{string} claims the held copy of {string}")
    public void claims(String member, String title) {
        claimsFor(member, title, 7);
    }

    @When("{string} claims the held copy of {string} for {int} days")
    public void claimsFor(String member, String title, int days) {
        Reply reply = api.send("POST", "/api/waitlist/" + entry(member, title) + "/claim?days=" + days, state.token(member), null);
        if (reply.status() == 201) {
            state.rememberLoan(member, title, api.json(reply).get("id").asLong());
        }
    }

    @When("{string} claims the held copy of {string} that is kept for {string}")
    public void claimsSomeoneElses(String member, String title, String owner) {
        api.send("POST", "/api/waitlist/" + entry(owner, title) + "/claim?days=7", state.token(member), null);
    }

    // ---------- the expiry job ----------

    @Given("{int} members each have a hold that ran out")
    public void holdsThatRanOut(int count) {
        for (int i = 0; i < count; i++) {
            TestBook book = data.newBook(1);
            data.setAvailable(book, 0);
            ranOutBooks.add(book);
            ranOut.add(data.newHoldEntry(data.newUser(), book, -10));
        }
    }

    @Given("another member has a hold that ends in an hour")
    public void holdNotOverYet() {
        notOverYetBook = data.newBook(1);
        data.setAvailable(notOverYetBook, 0);
        notOverYet = data.newHoldEntry(data.newUser(), notOverYetBook, 60);
    }

    @When("the hold expiry job runs")
    public void expiryJobRuns() {
        holdExpiryJob.expireHolds();
    }

    @Then("all those holds have expired and their copies are back on the shelf")
    public void allExpired() {
        for (int i = 0; i < ranOut.size(); i++) {
            assertEquals(WaitlistStatus.EXPIRED, data.reload(ranOut.get(i)).getStatus());
            assertEquals(1, data.inventory(ranOutBooks.get(i)).getAvailableCopies());
            data.assertStockConsistent(ranOutBooks.get(i));
        }
    }

    @Then("the hold that ends in an hour is still waiting to be claimed")
    public void notOverYetKept() {
        assertEquals(WaitlistStatus.HOLD_READY, data.reload(notOverYet).getStatus());
        assertEquals(0, data.inventory(notOverYetBook).getAvailableCopies());
    }

    // ---------- checks ----------

    @Then("{string} is number {int} in line")
    public void numberInLine(String member, int position) {
        JsonNode entry = api.lastJson();
        assertEquals("WAITING", entry.get("status").asString(), api.last().body());
        assertEquals(position, entry.get("position").asInt(), api.last().body());
    }

    @Then("the place of {string} on the waitlist of {string} is {string}")
    public void entryStatus(String member, String title, String status) {
        WaitlistEntry entry = waitlistRepository.findById(entry(member, title)).orElseThrow();
        assertEquals(status, entry.getStatus().name());
    }

    @Then("the copy of {string} is held for {string} for {int} hours")
    public void heldFor(String title, String member, int hours) {
        WaitlistEntry entry = waitlistRepository.findById(entry(member, title)).orElseThrow();
        assertEquals(WaitlistStatus.HOLD_READY, entry.getStatus());
        LocalDateTime expires = entry.getHoldExpiresAt();
        assertTrue(expires.isAfter(LocalDateTime.now().plusHours(hours).minusMinutes(5))
                && expires.isBefore(LocalDateTime.now().plusHours(hours).plusMinutes(5)), "the hold ends at " + expires);
    }

    @Then("the audit log records {string} for the hold of {string} on {string}")
    public void auditLogged(String action, String member, String title) {
        long entryId = entry(member, title);
        Reply log = api.send("GET", "/api/audit-log?type=WAITLIST&size=100", state.token("the librarian"), null);
        boolean found = false;
        for (JsonNode e : api.json(log).get("content")) {
            found |= e.get("action").asString().equals(action) && e.get("whatId").asLong() == entryId;
        }
        assertTrue(found, "no " + action + " entry for waitlist entry " + entryId + ": " + log.body());
    }

    private long entry(String member, String title) {
        return state.entry(member, title);
    }
}
