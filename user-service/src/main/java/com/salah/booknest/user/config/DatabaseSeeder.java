package com.salah.booknest.user.config;

import com.salah.booknest.user.model.User;
import com.salah.booknest.user.model.UserProfile;
import com.salah.booknest.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Seeds the sample users. They are saved in the same order as lending-service seeds its members,
 * so on empty databases the ids match (librarian = 1, johnd = 2, janed = 3, alexm = 4).
 * Their password comes from SEED_PASSWORD; it is only needed when the users table is still empty.
 */
@Component
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-password}")
    private String seedPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist. Skipping seeding.");
            return;
        }
        if (seedPassword.isBlank()) {
            throw new IllegalStateException("Set SEED_PASSWORD: it is the password of the sample users created on an empty database");
        }
        seedUser("librarian", "Salah", "Dawood", "librarian@example.com", "librarian", "I am a librarian", 22);
        seedUser("johnd", "John", "Doe", "johnd@example.com", "Member", "I LOVE BOOKS", randomAge());
        seedUser("janed", "Jane", "Smith", "janed@example.com", "Member", "I LOVE BOOKS", randomAge());
        seedUser("alexm", "Alex", "Miller", "alexm@example.com", "Member", "I LOVE BOOKS", randomAge());
        log.info("Users successfully seeded!");
    }

    private void seedUser(String username, String firstName, String lastName, String email, String role, String bio, int age) {
        User user = new User();
        user.setUsername(username);
        user.setEmailAddress(email);
        user.setIsVerified(true);
        user.setPassword(passwordEncoder.encode(seedPassword));
        user.setRole(role);

        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setBio(bio);
        profile.setAge(age);
        profile.setUser(user);
        user.setUserProfile(profile);

        userRepository.save(user);
    }

    private int randomAge() {
        return ThreadLocalRandom.current().nextInt(16, 76);
    }
}
