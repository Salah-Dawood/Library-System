package com.salah.booknest.notification.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

/**
 * The last event a scenario published: where it went, for whom, and its text,
 * so later steps can send it again or look for it. A new one is made for every scenario.
 */
@Component
@ScenarioScope
public class LastEvent {

    private String topic;
    private String username;
    private String json;
    private String eventId;
    private long publishedAt;

    public void set(String topic, String username, String json, String eventId, long publishedAt) {
        this.topic = topic;
        this.username = username;
        this.json = json;
        this.eventId = eventId;
        this.publishedAt = publishedAt;
    }

    /** When Kafka stored the event (epoch millis). */
    public long publishedAt() {
        return publishedAt;
    }

    public String topic() {
        return topic;
    }

    public String username() {
        return username;
    }

    public String json() {
        return json;
    }

    /** The eventId, or for a broken message (which has none) its text. */
    public String eventId() {
        return eventId;
    }
}
