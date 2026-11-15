package com.salah.booknest.gateway.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Starts the real gateway on a random port, with its routes pointing at the FakeServices servers.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfig {

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        for (String name : FakeServices.NAMES) {
            String property = name.equals("frontend") ? "frontend.url" : name + ".url";
            registry.add(property, () -> FakeServices.INSTANCE.url(name));
        }
    }
}
