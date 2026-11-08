package com.salah.booknest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class BooknestApplicationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext context;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Test
    void contextLoads() {
    }

    @Test
    void usesTheTestProfileAndAnInMemoryDatabase() {
        assertArrayEquals(new String[]{"test"}, environment.getActiveProfiles());
        assertTrue(datasourceUrl.startsWith("jdbc:h2:mem:"));
    }

    @Test
    void sampleDataSeedersAreNotRunInTests() {
        assertFalse(context.containsBean("databaseSeeder"));
        assertFalse(context.containsBean("sampleActivitySeeder"));
    }
}
