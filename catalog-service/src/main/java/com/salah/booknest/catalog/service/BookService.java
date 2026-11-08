package com.salah.booknest.catalog.service;

import com.salah.booknest.catalog.client.LendingClient;
import com.salah.booknest.catalog.exception.InformationExistException;
import com.salah.booknest.catalog.exception.InformationNotFoundException;
import com.salah.booknest.catalog.exception.InvalidRequestException;
import com.salah.booknest.catalog.model.Author;
import com.salah.booknest.catalog.model.Book;
import com.salah.booknest.catalog.model.Genre;
import com.salah.booknest.catalog.model.request.CreateBookRequest;
import com.salah.booknest.catalog.model.response.BookResponse;
import com.salah.booknest.catalog.model.response.StockResponse;
import com.salah.booknest.catalog.repository.AuthorRepository;
import com.salah.booknest.catalog.repository.BookRepository;
import com.salah.booknest.catalog.repository.GenreRepository;
import com.salah.booknest.catalog.repository.ReviewRepository;
import com.salah.booknest.catalog.util.PageUtils;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Books of the catalogue. The copy counts belong to lending-service, so they are fetched from it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "title", "publishedYear", "createdAt");

    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;
    private final ReviewRepository reviewRepository;
    private final LendingClient lendingClient;

    /**
     * Converts a book entity into the response sent to clients, adding author name, genre names,
     * stock, review count and the average rating rounded to one decimal.
     */
    public BookResponse bookResponser(Book book, StockResponse stock) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setAuthorName(book.getAuthor().getName());
        response.setPublishedYear(book.getPublishedYear());
        response.setIsbn(book.getIsbn());

        Set<Genre> bookGenres = book.getGenres();
        List<String> genres = new ArrayList<>();
        if (bookGenres != null) {
            genres = bookGenres.stream()
                    .map(Genre::getName)
                    .filter(Objects::nonNull)
                    .toList();
        }

        response.setGenreNames(genres);
        if (stock != null) {
            response.setTotalCopies(stock.totalCopies());
            response.setAvailableCopies(stock.availableCopies());
        }
        response.setReviewCount((int) reviewRepository.countByBookId(book.getId()));
        Double average = reviewRepository.averageRatingByBookId(book.getId());
        response.setAverageRating(average == null ? null : Math.round(average * 10) / 10.0);

        return response;
    }

    /**
     * Returns one page of books. The title filter matches part of the title and the genre filter
     * matches the exact genre name, both ignoring case. Page size is limited to 100.
     * The copy counts of the whole page are fetched from lending-service in one call.
     *
     * @throws InvalidRequestException if sortBy is not id, title, publishedYear or createdAt
     */
    @Transactional(readOnly = true)
    public Page<BookResponse> getBooks(String title, String genre, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageUtils.pageable(page, size, sortBy, sortDir, SORTABLE_FIELDS);

        Specification<Book> spec = (root, query, cb) -> cb.isTrue(root.get("active"));
        if (title != null && !title.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%"));
        }
        if (genre != null && !genre.isBlank()) {
            // Books and genres are many-to-many, so join and de-duplicate the books.
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<Book, Genre> genres = root.join("genres");
                return cb.equal(cb.lower(genres.get("name")), genre.trim().toLowerCase());
            });
        }
        Page<Book> books = bookRepository.findAll(spec, pageable);
        Map<Long, StockResponse> stock = lendingClient.getStock(books.map(Book::getId).getContent());
        return books.map(book -> bookResponser(book, stock.get(book.getId())));
    }

    public BookResponse getBookByTitle(String title) {
        return withStock(bookRepository.findByTitleAndActiveTrue(title)
                .orElseThrow(() -> new InformationNotFoundException("Active book with title " + title + " not found")));
    }

    public BookResponse getBookById(Long bookId) {
        return withStock(bookRepository.findByIdAndActiveTrue(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Active book with ID " + bookId + " not found")));
    }

    /**
     * Saves the book first, then asks lending-service to create its stock.
     * If lending-service refuses or is down, the new book is removed again so no book exists without stock.
     */
    public BookResponse createBook(CreateBookRequest request) {
        if (request.getTotalCopies() == null || request.getTotalCopies() < 0) {
            throw new InvalidRequestException("Total copies must be zero or more");
        }
        bookRepository.findByIsbn(request.getIsbn())
                .ifPresent(existingBook -> {
                    throw new InformationExistException("Book with ISBN " + request.getIsbn() + " already exists");
                });

        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book.setPublishedYear(request.getPublishedYear());

        if (request.getAuthorId() == null) {
            throw new InvalidRequestException("Author ID must be provided");
        }
        book.setAuthor(authorRepository.findByIdAndActiveTrue(request.getAuthorId())
                .orElseThrow(() -> new InformationNotFoundException("Active author not found with ID: " + request.getAuthorId())));
        book.setGenres(findGenres(request.getGenreIds()));

        Book savedBook = bookRepository.save(book);
        try {
            lendingClient.createStock(savedBook.getId(), request.getTotalCopies());
        } catch (RuntimeException e) {
            bookRepository.delete(savedBook);
            throw e;
        }
        log.info("Book {} created with {} copies", savedBook.getId(), request.getTotalCopies());
        return withStock(savedBook);
    }

    /**
     * Partial update: only the fields that are not null in the request are changed.
     * A new genre list replaces the old one, and a new total copies value is sent to lending-service.
     *
     * @throws InformationExistException if the ISBN already belongs to another book
     */
    @Transactional
    public BookResponse updateBook(Long bookId, CreateBookRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Book with ID " + bookId + " not found"));

        if (request.getIsbn() != null) {
            bookRepository.findByIsbn(request.getIsbn())
                    .filter(existing -> !existing.getId().equals(bookId))
                    .ifPresent(existing -> {
                        throw new InformationExistException("Book with ISBN " + request.getIsbn() + " already exists");
                    });
            book.setIsbn(request.getIsbn());
        }
        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getPublishedYear() != null) {
            book.setPublishedYear(request.getPublishedYear());
        }
        if (request.getAuthorId() != null) {
            book.setAuthor(authorRepository.findByIdAndActiveTrue(request.getAuthorId()).orElseThrow(
                    () -> new InformationNotFoundException("Active author not found with ID: " + request.getAuthorId())));
        }
        if (request.getGenreIds() != null) {
            book.setGenres(findGenres(request.getGenreIds()));
        }
        if (request.getTotalCopies() != null) {
            lendingClient.changeTotalCopies(bookId, request.getTotalCopies());
        }
        Book saved = bookRepository.save(book);
        log.info("Book {} updated", bookId);
        return withStock(saved);
    }

    /**
     * Deactivates a book without deleting its loans or reviews.
     * lending-service refuses while the book is on loan or a returned copy is still waiting for review.
     */
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Book with ID " + bookId + " not found"));

        if (Boolean.FALSE.equals(book.getActive())) {
            return;
        }

        lendingClient.retire(bookId);
        book.setActive(false);
        log.info("Book {} deactivated", bookId);
    }

    private BookResponse withStock(Book book) {
        return bookResponser(book, lendingClient.getStock(List.of(book.getId())).get(book.getId()));
    }

    private Set<Genre> findGenres(List<Long> genreIds) {
        Set<Genre> genres = new HashSet<>();
        if (genreIds != null) {
            for (Long genreId : genreIds) {
                genres.add(genreRepository.findById(genreId).orElseThrow(
                        () -> new InformationNotFoundException("Genre with id " + genreId + " not found")));
            }
        }
        return genres;
    }
}
