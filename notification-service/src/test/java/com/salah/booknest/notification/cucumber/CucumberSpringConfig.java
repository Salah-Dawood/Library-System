package com.salah.booknest.notification.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the whole notification-service on a random port (H2 database) together with a real Kafka broker
 * inside the test (no Docker). The scenarios publish events to Kafka exactly as the other services do.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 3, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Import(CucumberSpringConfig.TestMailConfig.class)
public class CucumberSpringConfig {

    /** Builds the emails as usual but keeps them in SentEmails instead of sending them. */
    @TestConfiguration
    static class TestMailConfig {

        @Bean
        SentEmails sentEmails() {
            return new SentEmails();
        }

        @Bean
        JavaMailSender javaMailSender(SentEmails sentEmails) {
            return new JavaMailSenderImpl() {
                @Override
                protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
                    for (MimeMessage message : mimeMessages) {
                        sentEmails.send(message);
                    }
                }
            };
        }
    }
}
