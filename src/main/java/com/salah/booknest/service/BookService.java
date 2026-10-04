package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Author;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Genre;
import com.salah.booknest.model.Inventory;
import com.salah.booknest.model.request.CreateBookRequest;
import com.salah.booknest.model.response.BookResponse;
import com.salah.booknest.repository.AuthorRepository;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.GenreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository,
                       GenreRepository genreRepository,
                       AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.genreRepository = genreRepository;
        this.authorRepository = authorRepository;
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
        response.setAvailableCopies(book.getInventory().getAvailableCopies());

        return response;
    }



    //return all book
    public List<BookResponse> getBooks(){
        List<Book> books = bookRepository.findAll();
        List<BookResponse> booksResponses = new ArrayList<>();
        for (Book book : books) {
            booksResponses.add(bookResponser(book));
        }
        return booksResponses;
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
    public ResponseEntity<?> createBook(CreateBookRequest request) {
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
            throw new IllegalArgumentException("Author ID must be provided");
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

        return new ResponseEntity<>(savedBook, HttpStatus.CREATED);
    }

    public ResponseEntity<?> updateBook(Long bookId,CreateBookRequest request){

        Book book = bookRepository.findById(bookId).orElseThrow(() -> new InformationNotFoundException("Book with ID " + bookId + " not found"));

       if (request.getIsbn() !=null) {
           bookRepository.findByIsbn(request.getIsbn())
                   .ifPresent(existingBook -> {
                       throw new InformationExistException("Book with ISBN " + request.getIsbn() + " already exists");
                   });
       }

        // book info
        if (request.getTitle()!=null) {
            book.setTitle(request.getTitle());
        }
        if (request.getIsbn() != null) {
            book.setIsbn(request.getIsbn());
        }
        if (request.getPublishedYear() != null) {
            book.setPublishedYear(request.getPublishedYear());
        }
        if (request.getAuthorId() != null) {
            Author author = authorRepository.findById(request.getAuthorId())
                    .orElseThrow(() -> new InformationNotFoundException("Author not found with ID: " + request.getAuthorId()));
            book.setAuthor(author);
        }

        Set<Genre> genres = new HashSet<>();
        if (request.getGenreIds() != null && !request.getGenreIds().isEmpty()) {
            for (Long genreId : request.getGenreIds()) {
                genres.add(genreRepository.findById(genreId).orElseThrow(() -> new InformationNotFoundException("Genre with id " + genreId + " not found")));
            }

            book.setGenres(genres);
        }

        Inventory inventory = book.getInventory();
        inventory.setTotalCopies(request.getTotalCopies());

        Book savedBook = bookRepository.save(book);

        return new ResponseEntity<>(savedBook, HttpStatus.OK);
    }

    public ResponseEntity<?> deleteBook(Long bookId){
        bookRepository.deleteById(bookId);
        return new ResponseEntity<>("Book Deleted",HttpStatus.NO_CONTENT);
    }


    //helper methods



}
