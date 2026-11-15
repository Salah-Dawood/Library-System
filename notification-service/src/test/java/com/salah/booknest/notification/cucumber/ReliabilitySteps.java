package com.salah.booknest.notification.cucumber;

import com.salah.booknest.notification.repository.ProcessedEventRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of kafka-reliability.feature: a mail server that fails, retries, the dead-letter topic and broken messages.
 */
public class ReliabilitySteps {

    // the retries are 2 seconds apart, so a retried event reaches the dead-letter topic after 6 seconds at the earliest
    private static final long RETRY_PAUSE_MS = 2000;

    @Autowired private Events events;
    @Autowired private LastEvent last;
    @Autowired private SentEmails sentEmails;
    @Autowired private ProcessedEventRepository processedEventRepository;

    private long movedAt = -1;

    @Given("the mail server fails the next {int} emails")
    public void mailServerFails(int times) {
        sentEmails.failNext(times);
    }

    @Given("the mail server is down")
    public void mailServerDown() {
        sentEmails.failNext(-1);
    }

    @When("the mail server is back")
    public void mailServerBack() {
        sentEmails.failNext(0);
    }

    @When("a broken message that is not JSON arrives on {string} for {string}")
    public void brokenMessage(String topic, String username) {
        String text = "{this is not json " + UUID.randomUUID();
        last.set(topic, username, text, text, events.publish(topic, username, text));
    }

    @Then("the event is moved to {string}")
    public void movedTo(String deadLetterTopic) {
        movedAt = events.findInTopic(deadLetterTopic, last.eventId(), 40);
        assertTrue(movedAt > 0, "the event never reached " + deadLetterTopic);
    }

    @Then("it was tried again before being moved")
    public void triedAgainFirst() {
        assertTrue(movedAt - last.publishedAt() >= 3 * RETRY_PAUSE_MS,
                "moved after " + (movedAt - last.publishedAt()) + " ms, too soon for 3 retries");
    }

    @Then("it was moved at once, without being tried again")
    public void movedAtOnce() {
        assertTrue(movedAt - last.publishedAt() < RETRY_PAUSE_MS,
                "moved after " + (movedAt - last.publishedAt()) + " ms, so it was retried");
    }

    @Then("sending the email was tried {int} times")
    public void emailTried(int times) {
        assertEquals(times, sentEmails.attempts());
    }

    @Then("the event is not marked as handled")
    public void notHandled() {
        assertFalse(processedEventRepository.existsById(last.eventId()));
    }
}
