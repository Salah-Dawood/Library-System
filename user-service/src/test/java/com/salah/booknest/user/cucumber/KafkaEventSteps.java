package com.salah.booknest.user.cucumber;

import com.salah.booknest.user.model.User;
import com.salah.booknest.user.repository.UserRepository;
import io.cucumber.java.en.Then;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of kafka-events.feature: what user-service publishes to the user-events topic.
 */
public class KafkaEventSteps {

    private static final String USER_EVENTS = "user-events";

    @Autowired private EmbeddedKafkaBroker broker;
    @Autowired private UserRepository userRepository;
    @Autowired private ApiClient api;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Then("a UserRegistered event for {string} with the email {string} is published")
    public void eventPublished(String username, String email) {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(username)).findFirst()
                .orElseThrow(() -> new AssertionError("no account " + username));

        ConsumerRecord<String, String> record = readUntil(r -> json(r).get("username").asString().equals(username))
                .stream().filter(r -> json(r).get("username").asString().equals(username)).findFirst()
                .orElseThrow(() -> new AssertionError("no UserRegistered event for " + username));

        JsonNode event = json(record);
        assertEquals(user.getId(), event.get("userId").asLong());
        assertEquals(email, event.get("email").asString());
        assertTrue(event.hasNonNull("eventId") && !event.get("eventId").asString().isBlank(), "the event has no eventId");
        assertTrue(event.hasNonNull("occurredAt"), "the event has no occurredAt");
        assertFalse(record.value().contains("password"), "the event must not carry the password: " + record.value());
        // the user id is the key, so all events of one user stay in order on one partition
        assertEquals(String.valueOf(user.getId()), record.key());
    }

    /**
     * A refused registration must publish nothing. A later registration is used as a marker:
     * once its event has arrived, an event for the refused one would have arrived before it.
     */
    @Then("no UserRegistered event with the email {string} is published")
    public void noEventPublished(String email) {
        String marker = "marker" + UUID.randomUUID().toString().substring(0, 8);
        api.send("POST", "/auth/users/register", null, api.json("username", marker, "emailAddress", marker + "@example.com",
                "password", "Passw0rd!", "firstName", "Mark", "lastName", "Er", "age", 30));
        assertEquals(201, api.lastStatus(), "the marker registration failed: " + api.lastBody());

        List<ConsumerRecord<String, String>> seen = readUntil(r -> json(r).get("username").asString().equals(marker));
        assertTrue(seen.stream().noneMatch(r -> email.equals(json(r).get("email").asString())),
                "an event was published for " + email);
    }

    /** Reads user-events from the start until a message matches, for at most 20 seconds; returns what it read. */
    private List<ConsumerRecord<String, String>> readUntil(Predicate<ConsumerRecord<String, String>> found) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-reader-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        List<ConsumerRecord<String, String>> seen = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(USER_EVENTS));
            long deadline = System.currentTimeMillis() + 20_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
                    seen.add(record);
                    if (found.test(record)) {
                        return seen;
                    }
                }
            }
        }
        throw new AssertionError("the expected event never arrived on " + USER_EVENTS + "; read " + seen.size() + " events");
    }

    private JsonNode json(ConsumerRecord<String, String> record) {
        return jsonMapper.readTree(record.value());
    }
}
