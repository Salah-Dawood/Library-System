package com.salah.booknest.lending.cucumber;

import com.salah.booknest.lending.event.LendingEvent;
import com.salah.booknest.lending.event.LendingEventPublisher;
import com.salah.booknest.lending.job.DueReminderJob;
import com.salah.booknest.lending.support.TestUser;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of kafka-events.feature: what lending-service published to loan-events and waitlist-events.
 */
public class KafkaEventSteps {

    @Autowired private KafkaTopics topics;
    @Autowired private LendingState state;
    @Autowired private DueReminderJob dueReminderJob;
    @Autowired private LendingEventPublisher publisher;
    @Autowired private TransactionTemplate transactionTemplate;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // ---------- events about a loan ----------

    @Then("{string} is told {string} about their loan of {string}")
    public void memberToldAboutLoan(String member, String type, String title) {
        ConsumerRecord<String, String> record = find(LendingEventPublisher.LOAN_EVENTS,
                loanEvent("MEMBER", type, state.loan(member, title)));
        assertEquals(state.member(member).username(), json(record).get("username").asString());
        // the member's id is the key, so that member's events stay in order on one partition
        assertEquals(String.valueOf(state.member(member).id()), record.key());
    }

    @Then("the librarians are told {string} about the loan of {string} by {string}")
    public void librariansTold(String type, String title, String member) {
        find(LendingEventPublisher.LOAN_EVENTS, loanEvent("LIBRARIANS", type, state.loan(member, title)));
    }

    @Then("the librarians are told {string} only once about the loan of {string} by {string}")
    public void librariansToldOnce(String type, String title, String member) {
        Predicate<ConsumerRecord<String, String>> match = loanEvent("LIBRARIANS", type, state.loan(member, title));
        List<ConsumerRecord<String, String>> all = topics.waitFor(LendingEventPublisher.LOAN_EVENTS, match);
        assertEquals(1, all.stream().filter(match).count(), "events: " + values(all));
    }

    // ---------- events about the waitlist ----------

    @Then("{string} is told {string} about the waitlist of {string}")
    public void memberToldAboutWaitlist(String member, String type, String title) {
        String username = state.member(member).username();
        JsonNode event = json(find(LendingEventPublisher.WAITLIST_EVENTS,
                r -> type.equals(json(r).get("type").asString()) && username.equals(json(r).get("username").asString())));
        assertTrue(event.get("message").asString().contains(state.book(title).title()), event.toString());
    }

    // ---------- the due reminder job ----------

    @When("the due reminder job runs")
    public void dueReminderJobRuns() {
        // the job waits until Kafka has confirmed every reminder
        dueReminderJob.sendDueReminders();
    }

    @Then("{string} is reminded that {string} is due tomorrow")
    public void reminded(String member, String title) {
        memberToldAboutLoan(member, "DUE_TOMORROW", title);
    }

    @Then("{string} is not reminded about {string}")
    public void notReminded(String member, String title) {
        long loanId = state.loan(member, title);
        assertTrue(topics.all(LendingEventPublisher.LOAN_EVENTS).stream()
                .noneMatch(loanEvent("MEMBER", "DUE_TOMORROW", loanId)), member + " was reminded");
    }

    // ---------- only after the commit ----------

    @When("a change that announces {string} to {string} is rolled back")
    public void rolledBack(String type, String member) {
        TestUser user = state.member(member);
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishAfterCommit(LendingEventPublisher.LOAN_EVENTS,
                    LendingEvent.forMember(type, user.id(), user.username(), 0L, "BORROWED", "never saved"));
            status.setRollbackOnly();
        });
    }

    @When("a change that announces {string} to {string} is saved")
    public void saved(String type, String member) {
        TestUser user = state.member(member);
        transactionTemplate.executeWithoutResult(status ->
                publisher.publishAfterCommit(LendingEventPublisher.LOAN_EVENTS,
                        LendingEvent.forMember(type, user.id(), user.username(), 0L, "BORROWED", "saved")));
    }

    /** The saved change comes after the rolled-back one for the same member, so it is a marker on the same partition. */
    @Then("{string} is told {string} but never {string}")
    public void toldButNever(String member, String saved, String rolledBack) {
        String username = state.member(member).username();
        List<ConsumerRecord<String, String>> all = topics.waitFor(LendingEventPublisher.LOAN_EVENTS,
                r -> saved.equals(json(r).get("type").asString()) && username.equals(json(r).get("username").asString()));
        assertTrue(all.stream().noneMatch(r -> rolledBack.equals(json(r).get("type").asString())
                && username.equals(json(r).get("username").asString())), "events: " + values(all));
    }

    // ---------- helpers ----------

    private Predicate<ConsumerRecord<String, String>> loanEvent(String audience, String type, long loanId) {
        return r -> {
            JsonNode e = json(r);
            return audience.equals(e.get("audience").asString()) && type.equals(e.get("type").asString())
                    && e.hasNonNull("loanId") && e.get("loanId").asLong() == loanId;
        };
    }

    private ConsumerRecord<String, String> find(String topic, Predicate<ConsumerRecord<String, String>> match) {
        return topics.waitFor(topic, match).stream().filter(match).findFirst().orElseThrow();
    }

    private JsonNode json(ConsumerRecord<String, String> record) {
        return jsonMapper.readTree(record.value());
    }

    private static List<String> values(List<ConsumerRecord<String, String>> records) {
        return records.stream().map(ConsumerRecord::value).toList();
    }
}
