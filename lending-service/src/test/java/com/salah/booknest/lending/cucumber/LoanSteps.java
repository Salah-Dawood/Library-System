package com.salah.booknest.lending.cucumber;

import com.salah.booknest.lending.cucumber.ApiClient.Reply;
import com.salah.booknest.lending.model.Inventory;
import com.salah.booknest.lending.model.Loan;
import com.salah.booknest.lending.repository.LoanRepository;
import com.salah.booknest.lending.support.TestBook;
import com.salah.booknest.lending.support.TestData;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of borrowing.feature and returns-and-reviews.feature, all through the real REST API.
 */
public class LoanSteps {

    @Autowired private ApiClient api;
    @Autowired private LendingState state;
    @Autowired private TestData data;
    @Autowired private LoanRepository loanRepository;

    // ---------- the library ----------

    @Given("the library has {int} copies of {string}")
    public void libraryHas(int copies, String title) {
        state.addBook(title, copies);
    }

    @Given("{string} already has {int} other books on loan")
    public void hasOtherLoans(String member, int count) {
        for (int i = 0; i < count; i++) {
            data.newLoan(state.member(member), data.newBook(1));
        }
    }

    @Given("{string} is no longer lent out")
    public void noLongerLentOut(String title) {
        Reply reply = api.send("DELETE", "/api/inventory/" + state.book(title).id(), state.token("the librarian"), null);
        assertEquals(204, reply.status(), "could not retire the book: " + reply.body());
    }

    // ---------- borrowing ----------

    @Given("{string} has borrowed {string}")
    public void hasBorrowed(String member, String title) {
        borrows(member, title, 7);
        assertEquals(201, api.last().status(), "could not borrow: " + api.last().body());
    }

    @Given("{string} has borrowed {string}, due {int} days ago")
    public void hasBorrowedOverdue(String member, String title, int days) {
        hasBorrowed(member, title);
        Loan loan = loanRepository.findById(state.loan(member, title)).orElseThrow();
        loan.setLoanDate(LocalDate.now().minusDays(7 + days));
        loan.setDueDate(LocalDate.now().minusDays(days));
        loanRepository.save(loan);
    }

    @Given("{string} has borrowed {string}, due in {int} days")
    public void hasBorrowedDueIn(String member, String title, int days) {
        hasBorrowed(member, title);
        Loan loan = loanRepository.findById(state.loan(member, title)).orElseThrow();
        loan.setDueDate(LocalDate.now().plusDays(days));
        loanRepository.save(loan);
    }

    @When("{string} borrows {string} for {int} days")
    public void borrows(String member, String title, int days) {
        Reply reply = api.send("POST", "/api/loans", state.token(member),
                "{\"bookId\":" + state.book(title).id() + ",\"duration\":" + days + "}");
        if (reply.status() == 201) {
            state.rememberLoan(member, title, api.json(reply).get("id").asLong());
        }
    }

    @When("{string} borrows {string}")
    public void borrows(String member, String title) {
        borrows(member, title, 7);
    }

    @When("someone borrows {string} without logging in")
    public void borrowsWithoutLogin(String title) {
        api.send("POST", "/api/loans", null, "{\"bookId\":" + state.book(title).id() + ",\"duration\":7}");
    }

    @When("{string} borrows without saying which book")
    public void borrowsNoBook(String member) {
        api.send("POST", "/api/loans", state.token(member), "{\"duration\":7}");
    }

    // ---------- returns, reviews, cancelling ----------

    @Given("{string} has returned {string}")
    public void hasReturned(String member, String title) {
        returns(member, title);
        assertEquals(200, api.last().status(), "could not return: " + api.last().body());
    }

    @When("{string} returns {string}")
    public void returns(String member, String title) {
        api.send("PUT", "/api/loans/" + state.loan(member, title) + "/return", state.token(member), null);
    }

    @When("the librarian records the return of {string} by {string}")
    public void librarianRecordsReturn(String title, String member) {
        api.send("PUT", "/api/loans/" + state.loan(member, title) + "/return", state.token("the librarian"), null);
        assertEquals(200, api.last().status(), "could not record the return: " + api.last().body());
    }

    @When("{string} returns the loan of {string} by {string}")
    public void returnsSomeoneElses(String member, String title, String borrower) {
        api.send("PUT", "/api/loans/" + state.loan(borrower, title) + "/return", state.token(member), null);
    }

    @When("the librarian reviews the return of {string} by {string} as not damaged")
    public void reviewNotDamaged(String title, String member) {
        review(title, member, "{\"damaged\":false}");
    }

    @When("the librarian reviews the return of {string} by {string} as damaged")
    public void reviewDamaged(String title, String member) {
        review(title, member, "{\"damaged\":true,\"notes\":\"Torn cover\"}");
    }

    @When("the librarian cancels the loan of {string} by {string}")
    public void cancel(String title, String member) {
        api.send("PUT", "/api/loans/" + state.loan(member, title) + "/cancel", state.token("the librarian"), null);
    }

    private void review(String title, String member, String json) {
        api.send("PUT", "/api/loans/" + state.loan(member, title) + "/review-return", state.token("the librarian"), json);
    }

    // ---------- checks ----------

    @Then("the loan is {string}")
    public void loanIs(String status) {
        assertEquals(status, api.lastJson().get("status").asString(), api.last().body());
    }

    @Then("the loan starts today and is due in {int} days")
    public void loanDates(int days) {
        JsonNode loan = api.lastJson();
        assertEquals(LocalDate.now().toString(), loan.get("loanDate").asString());
        assertEquals(LocalDate.now().plusDays(days).toString(), loan.get("dueDate").asString());
        assertEquals(days, loan.get("requestedDays").asInt());
    }

    @Then("the loan belongs to {string}")
    public void loanBelongsTo(String member) {
        assertEquals(state.member(member).username(), api.lastJson().get("username").asString());
    }

    @Then("the return was {string}")
    public void returnWas(String timing) {
        assertEquals(timing, api.lastJson().get("returnTiming").asString(), api.last().body());
    }

    @Then("it was {int} days late")
    public void daysLate(int days) {
        assertEquals(days, api.lastJson().get("daysLate").asInt(), api.last().body());
    }

    @Then("the return was reviewed by the librarian")
    public void reviewedByLibrarian() {
        assertEquals(state.librarian().username(), api.lastJson().get("reviewedBy").asString());
    }

    @Then("{string} has {int} of {int} copies on the shelf")
    public void copiesOnShelf(String title, int available, int total) {
        Inventory inventory = data.inventory(state.book(title));
        assertEquals(total, inventory.getTotalCopies(), "total copies");
        assertEquals(available, inventory.getAvailableCopies(), "copies on the shelf");
    }

    @Then("no loan of {string} was created")
    public void noLoan(String title) {
        TestBook book = state.book(title);
        assertTrue(loanRepository.findAll().stream().noneMatch(l -> l.getBookId().equals(book.id())));
    }

    @Then("{string} has a {string} fine of {double}")
    public void hasFine(String member, String type, double amount) {
        Reply fines = api.send("GET", "/api/fines/my-fines", state.token(member), null);
        boolean found = false;
        for (JsonNode fine : api.json(fines).get("content")) {
            found |= fine.get("type").asString().equals(type)
                    && new BigDecimal(fine.get("amount").asString()).compareTo(BigDecimal.valueOf(amount)) == 0;
        }
        assertTrue(found, member + " has no " + type + " fine of " + amount + ": " + fines.body());
    }

    @Then("{string} has no fines")
    public void hasNoFines(String member) {
        Reply fines = api.send("GET", "/api/fines/my-fines", state.token(member), null);
        assertEquals(0, api.json(fines).get("content").size(), fines.body());
    }

    @Then("the audit log records {string} for the loan of {string} by {string}")
    public void auditLogged(String action, String title, String member) {
        long loanId = state.loan(member, title);
        Reply log = api.send("GET", "/api/audit-log?type=LOAN&size=100", state.token("the librarian"), null);
        boolean found = false;
        for (JsonNode entry : api.json(log).get("content")) {
            found |= entry.get("action").asString().equals(action) && entry.get("whatId").asLong() == loanId;
        }
        assertTrue(found, "no " + action + " entry for loan " + loanId + ": " + log.body());
    }

    @Then("the shelf count of {string} adds up")
    public void stockConsistent(String title) {
        data.assertStockConsistent(state.book(title));
    }
}
