package com.salah.booknest.catalog.config;

import com.salah.booknest.catalog.exception.InformationNotFoundException;
import com.salah.booknest.catalog.model.Author;
import com.salah.booknest.catalog.model.Book;
import com.salah.booknest.catalog.model.Genre;
import com.salah.booknest.catalog.model.Review;
import com.salah.booknest.catalog.repository.AuthorRepository;
import com.salah.booknest.catalog.repository.BookRepository;
import com.salah.booknest.catalog.repository.GenreRepository;
import com.salah.booknest.catalog.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seeds the sample authors, genres, books and reviews. The books are saved in the same order as
 * lending-service seeds their stock, so on empty databases the book ids match (1 to 16).
 * The reviews belong to the sample users of user-service (johnd = 2, janed = 3, alexm = 4).
 */
@Component
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            log.info("Catalogue already has data. Skipping seeding.");
            return;
        }

        seedAuthor("George Orwell", 1903, "United Kingdom");
        seedAuthor("J.K. Rowling", 1965, "United Kingdom");
        seedAuthor("J.R.R. Tolkien", 1892, "United Kingdom");
        seedAuthor("Harper Lee", 1926, "USA");
        seedAuthor("Erich Maria Remarque", 1898, "Germany");
        seedAuthor("Isaac Asimov", 1920, "USA");
        seedAuthor("Agatha Christie", 1890, "United Kingdom");
        seedAuthor("Frank Herbert", 1920, "USA");

        seedGenre("Dystopian", "Speculative fiction exploring social and political structures."); // ID: 1
        seedGenre("Fantasy", "Magical elements, wonder, and mythical worlds.");               // ID: 2
        seedGenre("Fiction", "Narrative writing drawn from the imagination.");                 // ID: 3
        seedGenre("War", "Focuses on conflicts, military life, and political trauma.");        // ID: 4
        seedGenre("Sci-Fi", "Futuristic settings, advanced technology, and space exploration."); // ID: 5
        seedGenre("Mystery", "Solving a crime, dealing with secrets and investigations.");     // ID: 6
        seedGenre("Thriller", "Suspenseful, fast-paced narratives with high stakes.");         // ID: 7
        seedGenre("History", "Fictional accounts set against accurate historical backdrops.");   // ID: 8

        Book nineteen84 = seedBook("1984", "9780451524935", 1949, "George Orwell", List.of(1L, 3L, 5L));
        Book animalFarm = seedBook("Animal Farm", "9780451526342", 1945, "George Orwell", List.of(1L, 3L));
        seedBook("Harry Potter and the Sorcerer's Stone", "9780590353427", 1997, "J.K. Rowling", List.of(2L, 3L));
        seedBook("Harry Potter and the Order of the Phoenix", "9780439358071", 2003, "J.K. Rowling", List.of(2L, 6L));
        Book hobbit = seedBook("The Hobbit", "9780007487288", 1937, "J.R.R. Tolkien", List.of(2L));
        seedBook("The Fellowship of the Ring", "9780618346257", 1954, "J.R.R. Tolkien", List.of(2L, 4L));
        seedBook("The Two Towers", "9780618346264", 1954, "J.R.R. Tolkien", List.of(2L, 4L));
        Book mockingbird = seedBook("To Kill a Mockingbird", "9780446310789", 1960, "Harper Lee", List.of(3L, 6L, 8L));
        seedBook("All Quiet on the Western Front", "9780449213940", 1929, "Erich Maria Remarque", List.of(3L, 4L, 8L));
        seedBook("The Road Back", "9780449912461", 1931, "Erich Maria Remarque", List.of(3L, 4L));
        seedBook("Foundation", "9780553293357", 1951, "Isaac Asimov", List.of(1L, 5L));
        seedBook("I, Robot", "9780553382563", 1950, "Isaac Asimov", List.of(5L, 6L, 7L));
        seedBook("Murder on the Orient Express", "9780062073501", 1934, "Agatha Christie", List.of(6L, 7L));
        seedBook("And Then There Were None", "9780062073488", 1939, "Agatha Christie", List.of(3L, 6L, 7L));
        seedBook("Dune", "9780441172719", 1965, "Frank Herbert", List.of(1L, 2L, 4L, 5L));
        seedBook("Dune Messiah", "9780441172696", 1969, "Frank Herbert", List.of(1L, 4L, 5L));

        // Only members who returned a book can review it, so every review matches a returned loan seeded
        // by lending-service. johnd returned "Dune" but has not reviewed it yet, so writing a review can be shown live.
        seedReview(2L, "johnd", nineteen84, 9, "Chilling and still painfully relevant. Hard to put down.");
        seedReview(2L, "johnd", hobbit, 8, "A cozy adventure with great pacing.");
        seedReview(4L, "alexm", hobbit, 9, "The perfect way into Middle-earth.");
        seedReview(3L, "janed", animalFarm, 7, "Short, sharp and surprisingly funny in places.");
        seedReview(3L, "janed", mockingbird, 10, "A moving story with unforgettable characters.");

        log.info("Catalogue successfully seeded!");
    }

    private void seedAuthor(String name, Integer birthYear, String nationality) {
        Author author = new Author();
        author.setName(name);
        author.setBirthYear(birthYear);
        author.setNationality(nationality);
        authorRepository.save(author);
    }

    private void seedGenre(String name, String description) {
        Genre genre = new Genre();
        genre.setName(name);
        genre.setDescription(description);
        genreRepository.save(genre);
    }

    private Book seedBook(String title, String isbn, Integer year, String authorName, List<Long> genreIds) {
        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setPublishedYear(year);
        book.setAuthor(authorRepository.findByName(authorName)
                .orElseThrow(() -> new InformationNotFoundException("Author not found from seeder: " + authorName)));
        Set<Genre> genres = new HashSet<>();
        for (Long genreId : genreIds) {
            genres.add(genreRepository.findById(genreId)
                    .orElseThrow(() -> new InformationNotFoundException("Genre with id " + genreId + " not found from seeder")));
        }
        book.setGenres(genres);
        return bookRepository.save(book);
    }

    private void seedReview(Long userId, String username, Book book, int rating, String comment) {
        Review review = new Review();
        review.setUserId(userId);
        review.setUsername(username);
        review.setBook(book);
        review.setRating(rating);
        review.setComment(comment);
        reviewRepository.save(review);
    }
}
