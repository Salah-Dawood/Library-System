package com.salah.booknest.controller;

import com.salah.booknest.model.Book;
import com.salah.booknest.model.request.CreateBookRequest;
import com.salah.booknest.model.response.BookResponse;
import com.salah.booknest.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("")
    public ResponseEntity<Page<Book>> getAllBooks(
            // Optional Filtering Parameters
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,

            // Pagination Parameters (Defaults: Page 0, 10 items per page)
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,

            // Sorting Parameters (Defaults: Sort by 'id' in 'asc' order)
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<Book> booksPage = bookService.getBooks(title, genre, page, size, sortBy, sortDir);
        return ResponseEntity.ok(booksPage);
    }

    @GetMapping("/{bookId}")
    public BookResponse getBookById(@PathVariable Long bookId){
        return bookService.getBookById(bookId);
    }

    @GetMapping("/search/{title}")
    public BookResponse getBookByTitle(@PathVariable String title){
        return bookService.getBookByTitle(title);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> createBook(@Valid @RequestBody CreateBookRequest request){
        return bookService.createBook(request);
    }

    @PutMapping("/{bookId}")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity updateBook(@PathVariable Long bookId,
                                     @RequestBody CreateBookRequest request){
        return bookService.updateBook(bookId,request);
    }

    @DeleteMapping("/{bookId}")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> deleteBook(@PathVariable Long bookId){
        return bookService.deleteBook(bookId);
    }
}
