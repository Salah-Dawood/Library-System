package com.salah.booknest.notification.cucumber;

import com.salah.booknest.notification.repository.ProcessedEventRepository;
import com.salah.booknest.notification.repository.RecipientRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.salah.booknest.notification.cucumber.Events.waitUntil;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Steps of kafka-notifications.feature: events go into Kafka, and the checks look at what
 * notification-service did with them (recipients, emails, handled events).
 */
public class NotificationSteps {

    @Autowired private Events events;
    @Autowired private RecipientRepository recipientRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;
    @Autowired private SentEmails sentEmails;

    // the last lending event of this scenario, so it can be sent again
    private String lastTopic;
    private String lastUsername;
    private String lastJson;
    private String lastEventId;

    @Given("{string} registered with the email {string}")
    public void registered(String username, String email) {
        userServiceAnnounces(username, email);
        knowsTheEmail(username, email);
    }

    @When("user-service announces that {string} registered with the email {string}")
    public void userServiceAnnounces(String username, String email) {
        events.userRegistered(username, email);
    }

    @Then("notification-service knows that the email of {string} is {string}")
    public void knowsTheEmail(String username, String email) {
        waitUntil(() -> recipientRepository.findAll().stream()
                        .anyMatch(r -> r.getUsername().equals(username) && r.getEmail().equals(email)),
                "no recipient " + username + " with " + email + " was saved");
    }

    @When("lending-service announces {string} on {string} for {string}")
    public void lendingAnnounces(String type, String topic, String username) {
        announce(topic, type, "MEMBER", username);
    }

    @When("lending-service announces {string} for the librarians")
    public void lendingAnnouncesForLibrarians(String type) {
        announce(Events.LOAN_EVENTS, type, "LIBRARIANS", "johnd");
    }

    @When("lending-service announces the same event again")
    public void sameEventAgain() {
        events.publish(lastTopic, lastUsername, lastJson);
    }

    @Then("{string} receives an email {string}")
    public void receivesEmail(String address, String subject) {
        waitUntil(() -> sentEmails.all().stream().anyMatch(e -> e.to().equals(address) && e.subject().equals(subject)),
                "no email \"" + subject + "\" to " + address + "; sent: " + sentEmails.all());
    }

    @Then("the event is handled")
    public void eventIsHandled() {
        String eventId = lastEventId;
        waitUntil(() -> processedEventRepository.existsById(eventId), "event " + eventId + " was never handled");
    }

    @Then("{int} email(s) has/have been sent")
    public void emailsSent(int count) {
        everythingBeforeIsHandled();
        assertEquals(count, sentEmails.all().size(), "sent: " + sentEmails.all());
    }

    @Then("no email has been sent")
    public void noEmailSent() {
        emailsSent(0);
    }

    /**
     * Kafka keeps one member's events in order, so once a marker event sent after them is handled,
     * every event before it has been handled too. That makes "nothing more happened" a safe check.
     */
    private void everythingBeforeIsHandled() {
        String markerId = UUID.randomUUID().toString();
        events.publish(lastTopic, lastUsername,
                Events.lendingEvent(markerId, "MARKER", "LIBRARIANS", lastUsername, "end of the scenario"));
        waitUntil(() -> processedEventRepository.existsById(markerId), "the marker event was never handled");
    }

    private void announce(String topic, String type, String audience, String username) {
        lastTopic = topic;
        lastUsername = username;
        lastEventId = UUID.randomUUID().toString();
        lastJson = Events.lendingEvent(lastEventId, type, audience, username, "Update about your book");
        events.publish(topic, username, lastJson);
    }
}
