package com.salah.booknest.catalog.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Starts the whole catalog-service on a random port (H2 database, see application-test.properties)
 * so the scenarios call the real REST API. Its lending-service is the FakeLending server.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfig {

    @DynamicPropertySource
    static void lendingService(DynamicPropertyRegistry registry) {
        registry.add("lending-service.url", FakeLending.INSTANCE::url);
    }
}
