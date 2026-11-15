package com.salah.booknest.notification.cucumber;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
    private final EmbeddedKafkaBroker broker;

    public Events(EmbeddedKafkaBroker broker) {
        this.broker = broker;
        Map<String, Object> props = KafkaTestUtils.producerProps(broker);
        this.kafka = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()));
    }

    /**
     * Reads a topic from the start (a new consumer group each time) until a message containing {@code text}
     * shows up, for at most {@code seconds}. Returns the time Kafka stored that message (epoch millis), or -1.
     */
    public long findInTopic(String topic, String text, int seconds) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-reader-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + seconds * 1000L;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
                    if (record.value() != null && record.value().contains(text)) {
                        return record.timestamp();
                    }
                }
            }
            return -1;
        }
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

    /**
     * Sent with the user id as the key, like lending-service: one member's events stay in order on one partition.
     * Returns the time Kafka stored the message (epoch millis).
     */
    public long publish(String topic, String username, String json) {
        return kafka.send(topic, String.valueOf(userId(username)), json).join().getRecordMetadata().timestamp();
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
