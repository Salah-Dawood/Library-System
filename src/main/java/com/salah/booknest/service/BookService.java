package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Author;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.Genre;
import com.salah.booknest.repository.AuthorRepository;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.GenreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

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


    //return all book
    public List<Book> getBooks(){
        return bookRepository.findAll();
    }

    //return Book by title
    public Book getBookByTitle(String title){
        return bookRepository.findByTitle(title)
                .orElseThrow(() -> new InformationNotFoundException("book with title " + title + " not found"));
    }

    //return Book by ID
    public Book getBookById(Long bookId){
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new InformationNotFoundException("book with ID " + bookId + " not found"));
    }


    //create book
    public ResponseEntity<?> createBook(Book bookObject) {
        bookRepository.findByIsbn(bookObject.getIsbn())
                .ifPresent(existingBook -> {
                    throw new InformationExistException("Book with ISBN " + bookObject.getIsbn() + " already exists");
                });

        Book book = new Book();
        book.setTitle(bookObject.getTitle());
        book.setIsbn(bookObject.getIsbn());
        book.setPublishedYear(bookObject.getPublishedYear());

        if (bookObject.getAuthor() != null && bookObject.getAuthor().getId() != null) {
            Author managedAuthor = authorRepository.findById(bookObject.getAuthor().getId())
                    .orElseThrow(() -> new InformationNotFoundException("Author not found with ID: " + bookObject.getAuthor().getId()));
            book.setAuthor(managedAuthor);
        } else {
            throw new IllegalArgumentException("Author ID must be provided");
        }

        if (bookObject.getGenres() != null && !bookObject.getGenres().isEmpty()) {
            for (Genre requestedGenre : bookObject.getGenres()) {
                genreRepository.findById(requestedGenre.getId())
                        .ifPresent(managedGenre -> {

                            book.getGenres().add(managedGenre);
                        });
            }
        }
        Book savedBook = bookRepository.save(book);

        return new ResponseEntity<>(savedBook, HttpStatus.CREATED);
    }


}
