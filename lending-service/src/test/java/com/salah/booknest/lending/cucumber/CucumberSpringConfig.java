package com.salah.booknest.lending.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the whole lending-service on a random port (H2 database, see application-test.properties)
 * so the scenarios call the real REST API, with a real Kafka broker inside the test for its events.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, topics = {"loan-events", "waitlist-events"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public class CucumberSpringConfig {
}
