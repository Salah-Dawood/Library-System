package com.salah.booknest.config;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.*;
import com.salah.booknest.repository.AuthorRepository;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.GenreRepository;
import com.salah.booknest.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final BookRepository bookRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {

            //librarian
            seedLibrarian("librarian","Salah","Dawood","salah.dawood364@gmail.com");

            // 2.members
            seedMember("johnd", "John", "Doe", "john.doe@gmail.com");
            seedMember("janed", "Jane", "Smith", "jane.smith@yahoo.com");
            seedMember("alexm", "Alex", "Miller", "alex.miller@outlook.com");
            seedMember("emilyw", "Emily", "Watson", "emily.w@gmail.com");
            seedMember("michaelb", "Michael", "Brown", "mbrown@gmail.com");

            //authors
            seedAuthor("George Orwell", 1903, "United Kingdom");
            seedAuthor("J.K. Rowling", 1965, "United Kingdom");
            seedAuthor("J.R.R. Tolkien", 1892, "United Kingdom");
            seedAuthor("Harper Lee", 1926, "USA");
            seedAuthor("Erich Maria Remarque", 1898, "Germany");
            seedAuthor("Isaac Asimov", 1920, "USA");
            seedAuthor("Agatha Christie", 1890, "United Kingdom");
            seedAuthor("Frank Herbert", 1920, "USA");

            // 4. Genres (Note: IDs will be assigned sequentially by your DB: 1, 2, 3, etc.)
            seedGenre("Dystopian", "Speculative fiction exploring social and political structures."); // ID: 1
            seedGenre("Fantasy", "Magical elements, wonder, and mythical worlds.");               // ID: 2
            seedGenre("Fiction", "Narrative writing drawn from the imagination.");                 // ID: 3
            seedGenre("War", "Focuses on conflicts, military life, and political trauma.");        // ID: 4
            seedGenre("Sci-Fi", "Futuristic settings, advanced technology, and space exploration."); // ID: 5
            seedGenre("Mystery", "Solving a crime, dealing with secrets and investigations.");     // ID: 6
            seedGenre("Thriller", "Suspenseful, fast-paced narratives with high stakes.");         // ID: 7
            seedGenre("History", "Fictional accounts set against accurate historical backdrops.");   // ID: 8

            // books)
            // George Orwell
            seedBook("1984", "9780451524935", 1949, "George Orwell", List.of(1L, 3L, 5L)); // Dystopian, Fiction, Sci-Fi
            seedBook("Animal Farm", "9780451526342", 1945, "George Orwell", List.of(1L, 3L)); // Dystopian, Fiction

            // J.K. Rowling
            seedBook("Harry Potter and the Sorcerer's Stone", "9780590353427", 1997, "J.K. Rowling", List.of(2L, 3L)); // Fantasy, Fiction
            seedBook("Harry Potter and the Order of the Phoenix", "9780439358071", 2003, "J.K. Rowling", List.of(2L, 6L)); // Fantasy, Mystery

            // J.R.R. Tolkien
            seedBook("The Hobbit", "9780007487288", 1937, "J.R.R. Tolkien", List.of(2L)); // Fantasy
            seedBook("The Fellowship of the Ring", "9780618346257", 1954, "J.R.R. Tolkien", List.of(2L, 4L)); // Fantasy, War (War of the Ring context)
            seedBook("The Two Towers", "9780618346264", 1954, "J.R.R. Tolkien", List.of(2L, 4L)); // Fantasy, War

            // Harper Lee
            seedBook("To Kill a Mockingbird", "9780446310789", 1960, "Harper Lee", List.of(3L, 6L, 8L)); // Fiction, Mystery (the trial), History

            // Erich Maria Remarque
            seedBook("All Quiet on the Western Front", "9780449213940", 1929, "Erich Maria Remarque", List.of(3L, 4L, 8L)); // Fiction, War, History
            seedBook("The Road Back", "9780449912461", 1931, "Erich Maria Remarque", List.of(3L, 4L)); // Fiction, War (Post-war trauma)

            // Isaac Asimov
            seedBook("Foundation", "9780553293357", 1951, "Isaac Asimov", List.of(1L, 5L)); // Dystopian (Fall of Empire), Sci-Fi
            seedBook("I, Robot", "9780553382563", 1950, "Isaac Asimov", List.of(5L, 6L, 7L)); // Sci-Fi, Mystery (detective framing), Thriller

            // Agatha Christie
            seedBook("Murder on the Orient Express", "9780062073501", 1934, "Agatha Christie", List.of(6L, 7L)); // Mystery, Thriller
            seedBook("And Then There Were None", "9780062073488", 1939, "Agatha Christie", List.of(3L, 6L, 7L)); // Fiction, Mystery, Thriller

            // Frank Herbert
            seedBook("Dune", "9780441172719", 1965, "Frank Herbert", List.of(1L, 2L, 4L, 5L)); // Dystopian, Fantasy, War, Sci-Fi
            seedBook("Dune Messiah", "9780441172696", 1969, "Frank Herbert", List.of(1L, 4L, 5L)); // Dystopian, War, Sci-Fi

            System.out.println("Database successfully seeded!");
        } else {
            System.out.println("Database already has data. Skipping seeding.");
        }
    }

    public void seedLibrarian(String username,String firstName,String lastName,String email){
        User librarian = new User();
        librarian.setUsername(username);
        librarian.setEmailAddress(email);
        librarian.setIsVerified(true);
        librarian.setPassword(passwordEncoder.encode("password"));
        librarian.setRole("librarian");

        //Seed Librarian Profile
        UserProfile libProfile = new UserProfile();
        libProfile.setFirstName(firstName);
        libProfile.setLastName(lastName);
        libProfile.setBio("I am a librarian");
        libProfile.setAge(22);
        libProfile.setUser(librarian);
        librarian.setUserProfile(libProfile);

        userRepository.save(librarian);
    }

    public void seedMember(String username,String firstName,String lastName,String email){
        //Seed a Member Account
        User member = new User();
        member.setUsername(username);
        member.setEmailAddress(email);
        member.setIsVerified(true);
        member.setPassword(passwordEncoder.encode("password"));

        //Seed Member Profile
        UserProfile memProfile = new UserProfile();
        memProfile.setFirstName(firstName);
        memProfile.setLastName(lastName);
        memProfile.setAge(randomAge());
        memProfile.setBio("I LOVE BOOKS");
        memProfile.setUser(member);
        member.setUserProfile(memProfile);

        userRepository.save(member);
    }

    public int randomAge(){
        return ThreadLocalRandom.current().nextInt(16, 76);
    }

    public void seedAuthor(String name,Integer birthYear,String nationality){
        Author author = new Author();

        author.setName(name);
        author.setBirthYear(birthYear);
        author.setNationality(nationality);

        authorRepository.save(author);
    }

    public void seedGenre(String name,String description){
        Genre genre = new Genre();

        genre.setName(name);
        genre.setDescription(description);

        genreRepository.save(genre);
    }

    @Transactional
    public void seedBook(String title,String isbn,Integer year,String authorName,List<Long> genreIds){
        Book book = new Book();

        book.setTitle(title);
        book.setIsbn(isbn);
        book.setPublishedYear(year);

        book.setAuthor(authorRepository.findByName(authorName).orElseThrow(() -> new InformationNotFoundException("Author not found from seeder: " + authorName)));
        Set<Genre> genres = new HashSet<>();

        for (Long genreId : genreIds) {
            genres.add(genreRepository.findById(genreId).orElseThrow(() -> new InformationNotFoundException("Genre with id " + genreId + " not found from seeder")));
        }
        book.setGenres(genres);

        Inventory inventory = new Inventory();
        Integer copies = randomCopies();
        inventory.setBook(book);
        inventory.setTotalCopies(copies);
        inventory.setAvailableCopies(copies);

        book.setInventory(inventory);
        bookRepository.save(book);

    }

    public int randomCopies(){
        return ThreadLocalRandom.current().nextInt(5, 20);
    }
}