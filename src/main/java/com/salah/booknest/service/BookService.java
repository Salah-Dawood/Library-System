package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.model.Author;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Genre;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.request.CreateBookRequest;
import com.salah.booknest.model.response.BookResponse;
import com.salah.booknest.repository.AuthorRepository;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.GenreRepository;
import com.salah.booknest.repository.InventoryRepository;
import com.salah.booknest.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;
    private final InventoryRepository inventoryRepository;
    private final ReviewRepository reviewRepository;
    private final AuditLogService auditLogService;

    public BookService(BookRepository bookRepository,
                       GenreRepository genreRepository,
                       AuthorRepository authorRepository,
                       InventoryRepository inventoryRepository,
                       ReviewRepository reviewRepository,
                       AuditLogService auditLogService) {
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.authorRepository = authorRepository;
        this.inventoryRepository = inventoryRepository;
        this.reviewRepository = reviewRepository;
        this.auditLogService = auditLogService;
    }

    public BookResponse bookResponser(Book book){
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
        response.setTotalCopies(book.getInventory().getTotalCopies());
        response.setAvailableCopies(book.getInventory().getAvailableCopies());
        response.setReviewCount((int) reviewRepository.countByBookId(book.getId()));
        Double average = reviewRepository.averageRatingByBookId(book.getId());
        response.setAverageRating(average == null ? null : Math.round(average * 10) / 10.0);

        return response;
    }



    //return all book
    public Page<Book> getBooks(String title, String genre, int page, int size, String sortBy, String sortDir) {

        // 1. SET UP PAGINATION AND SORTING
        // Chooses ASC or DESC sorting order
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        // Creates the Pageable controller config (Spring uses 0-indexed pages)
        Pageable pageable = PageRequest.of(page, size, sort);

        // 2. SET UP DYNAMIC FILTERING (JPA Specification)
        Specification<Book> spec = null;

        if (title != null && !title.trim().isEmpty()) {
            Specification<Book> titleSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");

            // Combine cleanly using a quick null check conditions loop
            spec = (spec == null) ? Specification.where(titleSpec) : spec.and(titleSpec);
        }

        if (genre != null && !genre.trim().isEmpty()) {
            Specification<Book> genreSpec = (root, query, cb) -> cb.equal(root.get("genre"), genre);
            spec = (spec == null) ? Specification.where(genreSpec) : spec.and(genreSpec);
        }

        // 3. EXECUTE COMBINED DB QUERY
        return bookRepository.findAll(spec, pageable);
    }

    //return Book by title
    public BookResponse getBookByTitle(String title){
        return bookResponser(bookRepository.findByTitle(title)
                .orElseThrow(() -> new InformationNotFoundException("book with title " + title + " not found")));
    }

    //return Book by ID
    public BookResponse getBookById(Long bookId){
        return bookResponser(bookRepository.findById(bookId)
                .orElseThrow(() -> new InformationNotFoundException("book with ID " + bookId + " not found")));
    }


    //create book
    @Transactional
    public ResponseEntity<?> createBook(CreateBookRequest request) {
        if (request.getTotalCopies() == null || request.getTotalCopies() < 0) {
            throw new InvalidRequestException("Total copies must be zero or more");
        }
        bookRepository.findByIsbn(request.getIsbn())
                .ifPresent(existingBook -> {
                    throw new InformationExistException("Book with ISBN " + request.getIsbn() + " already exists");
                });

        Book book = new Book();
        // book info
        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book.setPublishedYear(request.getPublishedYear());

        // author
        if (request.getAuthorId() != null) {
            Author author = authorRepository.findById(request.getAuthorId())
                    .orElseThrow(() -> new InformationNotFoundException("Author not found with ID: " + request.getAuthorId()));
            book.setAuthor(author);
        } else {
            throw new InvalidRequestException("Author ID must be provided");
        }

        //book genres
        Set<Genre> genres = new HashSet<>();
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            for (Long genreId : request.getGenreIds()) {
                genres.add(genreRepository.findById(genreId).orElseThrow(() -> new InformationNotFoundException("Genre with id " + genreId + " not found")));
            }

            book.setGenres(genres);
        }

        //create inventory
        Inventory inventory = new Inventory();
        inventory.setBook(book);
        inventory.setTotalCopies(request.getTotalCopies());
        inventory.setAvailableCopies(request.getTotalCopies());

        book.setInventory(inventory);

        Book savedBook = bookRepository.save(book);
        auditLogService.log("BOOK", "CREATED", savedBook.getId());

        return new ResponseEntity<>(bookResponser(savedBook), HttpStatus.CREATED);
    }

    @Transactional
    public ResponseEntity<?> updateBook(Long bookId, CreateBookRequest request) {
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
            book.setAuthor(authorRepository.findById(request.getAuthorId()).orElseThrow(
                    () -> new InformationNotFoundException("Author not found with ID: " + request.getAuthorId())));
        }
        if (request.getGenreIds() != null) {
            Set<Genre> genres = new HashSet<>();
            for (Long genreId : request.getGenreIds()) {
                genres.add(genreRepository.findById(genreId).orElseThrow(
                        () -> new InformationNotFoundException("Genre with id " + genreId + " not found")));
            }
            book.setGenres(genres);
        }
        if (request.getTotalCopies() != null) {
            updateTotalCopies(bookId, request.getTotalCopies());
        }
        Book saved = bookRepository.save(book);
        auditLogService.log("BOOK", "UPDATED", saved.getId());
        return new ResponseEntity<>(bookResponser(saved), HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<Void> deleteBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new InformationNotFoundException("Book with ID " + bookId + " not found");
        }
        // Audited first so the entry can still name the book it is about.
        auditLogService.log("BOOK", "DELETED", bookId);
        bookRepository.deleteById(bookId);
        return ResponseEntity.noContent().build();
    }


    private void updateTotalCopies(Long bookId, int newTotal) {
        Inventory inventory = inventoryRepository.findByBookIdForUpdate(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Inventory for book " + bookId + " not found"));
        int onLoan = inventory.getTotalCopies() - inventory.getAvailableCopies();
        if (newTotal < onLoan) {
            throw new InvalidRequestException(
                    "Total copies cannot be lower than the " + onLoan + " copies currently on loan");
        }
        inventory.setTotalCopies(newTotal);
        inventory.setAvailableCopies(newTotal - onLoan);
    }

    //helper methods



}
