package com.salah.booknest.lending.cucumber;

import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of configuration.feature: which profile and settings the running service picked up.
 */
public class ConfigurationSteps {

    @Autowired private Environment environment;
    @Autowired private ApplicationContext context;

    @Then("the only active profile is {string}")
    public void onlyActiveProfile(String profile) {
        assertArrayEquals(new String[]{profile}, environment.getActiveProfiles());
    }

    @Then("the database is an in-memory database")
    public void inMemoryDatabase() {
        String url = environment.getProperty("spring.datasource.url");
        assertTrue(url != null && url.startsWith("jdbc:h2:mem:"), "the database is " + url);
    }

    @Then("the sample data is not loaded")
    public void noSampleData() {
        assertFalse(context.containsBean("databaseSeeder"), "the stock seeder ran");
        assertFalse(context.containsBean("sampleActivitySeeder"), "the sample loans seeder ran");
    }

    @Then("the race demo endpoints are switched off")
    public void raceDemoOff() {
        assertFalse(context.containsBean("raceDemoController"), "the race demo is on");
    }

    @Then("the setting {string} is {string}")
    public void settingIs(String key, String value) {
        assertTrue(value.equals(environment.getProperty(key)), key + " is " + environment.getProperty(key));
    }
}
