package com.salah.booknest.notification.config;

import com.salah.booknest.notification.model.Recipient;
import com.salah.booknest.notification.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds the email addresses of the sample users of user-service (same ids: librarian = 1 to alexm = 4).
 * Users who register later arrive through UserRegistered events.
 */
@Component
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RecipientRepository recipientRepository;

    @Override
    public void run(String... args) {
        if (recipientRepository.count() > 0) {
            log.info("Recipients already exist. Skipping seeding.");
            return;
        }
        recipientRepository.save(new Recipient(1L, "librarian", "salah.dawood364@gmail.com"));
        recipientRepository.save(new Recipient(2L, "johnd", "202200240@student.polytechnic.bh"));
        recipientRepository.save(new Recipient(3L, "janed", "jane.smith@yahoo.com"));
        recipientRepository.save(new Recipient(4L, "alexm", "alex.miller@outlook.com"));
        log.info("Recipients successfully seeded!");
    }
}
