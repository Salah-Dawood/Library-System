package com.salah.booknest.user.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the whole user-service on a random port (H2 database, see application-test.properties)
 * so the scenarios call the real REST API.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(CucumberSpringConfig.NoEmailConfig.class)
public class CucumberSpringConfig {

    /** Builds the emails as usual but never sends them. */
    @TestConfiguration
    static class NoEmailConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return new JavaMailSenderImpl() {
                @Override
                protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
                }
            };
        }
    }
}
