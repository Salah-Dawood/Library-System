package com.salah.booknest.notification.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static com.salah.booknest.notification.cucumber.Events.waitUntil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Steps of live-notifications.feature: open streams and the events pushed into them.
 */
public class LiveNotificationSteps {

    @Autowired private LiveStreams streams;
    @Autowired private Tokens tokens;

    @After
    public void closeStreams() {
        streams.closeAll();
    }

    @Given("{string} has the live notifications open")
    public void hasStreamOpen(String username) {
        streams.open(username, tokens.of(username));
        waitUntil(() -> streams.received(username, "CONNECTED"), username + "'s stream never connected");
    }

    @Then("{string} sees a live {string} notification")
    public void seesNotification(String username, String type) {
        waitUntil(() -> streams.received(username, type),
                username + " never saw " + type + "; the stream got " + streams.lines(username));
    }

    @Then("{string} saw the {string} notification once")
    public void sawOnce(String username, String type) {
        long times = streams.lines(username).stream().filter(line -> line.replace(" ", "").equals("event:" + type)).count();
        assertEquals(1, times, username + "'s stream got " + streams.lines(username));
    }

    @Then("{string} sees no live {string} notification")
    public void seesNoNotification(String username, String type) {
        assertFalse(streams.received(username, type), username + "'s stream got " + streams.lines(username));
    }
}
