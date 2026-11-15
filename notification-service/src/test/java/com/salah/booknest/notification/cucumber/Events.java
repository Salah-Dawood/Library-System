package com.salah.booknest.notification.cucumber;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Publishes events to the embedded Kafka broker with the same JSON as user-service and lending-service,
 * and waits for notification-service to react.
 */
@Component
public class Events {

    public static final String USER_EVENTS = "user-events";
    public static final String LOAN_EVENTS = "loan-events";
    public static final String WAITLIST_EVENTS = "waitlist-events";

    private final KafkaTemplate<String, String> kafka;

    public Events(EmbeddedKafkaBroker broker) {
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        this.kafka = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()));
    }

    /** The sample users have the same ids as in user-service: johnd 2, janed 3, alexm 4. */
    public static long userId(String username) {
        return switch (username) {
            case "librarian" -> 1L;
            case "johnd" -> 2L;
            case "janed" -> 3L;
            case "alexm" -> 4L;
            default -> 99L;
        };
    }

    public void userRegistered(String username, String email) {
        String json = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"userId\":" + userId(username)
                + ",\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"occurredAt\":\"2026-11-15T10:00:00\"}";
        publish(USER_EVENTS, username, json);
    }

    /** A LendingEvent as lending-service sends it; audience is MEMBER or LIBRARIANS. */
    public static String lendingEvent(String eventId, String type, String audience, String username, String message) {
        return "{\"eventId\":\"" + eventId + "\",\"type\":\"" + type + "\",\"audience\":\"" + audience
                + "\",\"userId\":" + userId(username) + ",\"username\":\"" + username + "\",\"loanId\":7"
                + ",\"status\":\"BORROWED\",\"message\":\"" + message + "\",\"occurredAt\":\"2026-11-15T10:00:00\"}";
    }

    /** Sent with the user id as the key, like lending-service: one member's events stay in order on one partition. */
    public void publish(String topic, String username, String json) {
        kafka.send(topic, String.valueOf(userId(username)), json).join();
    }

    /** Waits up to 20 seconds for the condition, then fails with the message. */
    public static void waitUntil(BooleanSupplier condition, String failure) {
        long deadline = System.currentTimeMillis() + 20_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError(failure);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while waiting: " + failure);
            }
        }
    }
}
