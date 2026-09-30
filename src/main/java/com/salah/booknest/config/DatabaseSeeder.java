package com.salah.booknest.config;

import com.salah.booknest.model.User;
import com.salah.booknest.model.UserProfile;
import com.salah.booknest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {

            //Seed a Librarian Account
            User librarian = new User();
            librarian.setUsername("librarian");
            librarian.setEmailAddress("librarian@gmail.com");
            librarian.setIsVerified(true);
            librarian.setPassword(passwordEncoder.encode("password"));
            librarian.setRole("librarian");

            //Seed Librarian Profile
            UserProfile libProfile = new UserProfile();
            libProfile.setFirstName("Salah");
            libProfile.setLastName("Dawood");
            libProfile.setBio("I am the Main librarian");
            libProfile.setUser(librarian);
            librarian.setUserProfile(libProfile);

            userRepository.save(librarian);

            //Seed a Member Account
            User member = new User();
            member.setUsername("john");
            member.setEmailAddress("john@gmail.com");
            member.setIsVerified(true);
            member.setPassword(passwordEncoder.encode("password"));

            //Seed Member Profile
            UserProfile memProfile = new UserProfile();
            memProfile.setFirstName("John");
            memProfile.setLastName("Doe");
            memProfile.setAge(18);
            memProfile.setBio("I LOVE BOOKS");
            memProfile.setUser(member);
            member.setUserProfile(memProfile);

            userRepository.save(member);

            System.out.println("Database successfully seeded with 1 Librarian and 1 Member!");
        } else {
            System.out.println("Database already has data. Skipping user seeding.");
        }
    }
}