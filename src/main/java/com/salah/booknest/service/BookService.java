package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Author;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Genre;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.request.CreateBookRequest;
import com.salah.booknest.model.response.BookResponse;
import com.salah.booknest.repository.AuthorRepository;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.GenreRepository;
import com.salah.booknest.repository.InventoryRepository;
import com.salah.booknest.repository.LoanRepository;
import com.salah.booknest.repository.ReviewRepository;
import com.salah.booknest.util.PageUtils;
import jakarta.persistence.criteria.Join;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final LoanRepository loanRepository;
    private final AuditLogService auditLogService;

    public BookService(BookRepository bookRepository,
                       GenreRepository genreRepository,
                       AuthorRepository authorRepository,
                       InventoryRepository inventoryRepository,
                       ReviewRepository reviewRepository,
                       LoanRepository loanRepository,
                       AuditLogService auditLogService) {
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.authorRepository = authorRepository;
        this.inventoryRepository = inventoryRepository;
        this.reviewRepository = reviewRepository;
        this.loanRepository = loanRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Converts a book entity into the response sent to clients, adding author name, genre names,
     * stock, review count and the average rating rounded to one decimal.
     */
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
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "title", "publishedYear", "createdAt");


    /**
     * Returns one page of books. The title filter matches part of the title and the genre filter
     * matches the exact genre name, both ignoring case. Page size is limited to 100.
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
        return bookRepository.findAll(spec, pageable).map(this::bookResponser);
    }

    //return Book by title
    public BookResponse getBookByTitle(String title){
        return bookResponser(bookRepository.findByTitleAndActiveTrue(title)
                .orElseThrow(() -> new InformationNotFoundException("Active book with title " + title + " not found")));
    }

    //return Book by ID
    public BookResponse getBookById(Long bookId){
        return bookResponser(bookRepository.findByIdAndActiveTrue(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Active book with ID " + bookId + " not found")));
    }


    //create book
    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
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
            Author author = authorRepository.findByIdAndActiveTrue(request.getAuthorId())
                    .orElseThrow(() -> new InformationNotFoundException("Active author not found with ID: " + request.getAuthorId()));
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

        return bookResponser(savedBook);
    }

    /**
     * Partial update: only the fields that are not null in the request are changed.
     * A new genre list replaces the old one, and a new total copies value adjusts stock (see updateTotalCopies).
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
        return bookResponser(saved);
    }

    /**
     * Deactivates a book without deleting its loans or reviews.
     * A book cannot be deactivated while it is on loan or a returned copy is still waiting for review.
     */
    @Transactional
    public void deleteBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new InformationNotFoundException("Book with ID " + bookId + " not found"));

        if (Boolean.FALSE.equals(book.getActive())) {
            return;
        }

        boolean hasActiveLoans = loanRepository.existsByBookIdAndStatusIn(
                bookId, List.of(LoanStatus.BORROWED, LoanStatus.PENDING_REVIEW));
        if (hasActiveLoans) {
            throw new InvalidStateException("Book is on loan or has returns waiting for review and cannot be removed");
        }

        book.setActive(false);
        auditLogService.log("BOOK", "DEACTIVATED", bookId);
    }


    /**
     * Changes the total number of copies while keeping the copies currently on loan unchanged:
     * available copies become the new total minus the copies on loan.
     *
     * @throws InvalidRequestException if the new total is lower than the number of copies on loan
     */
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