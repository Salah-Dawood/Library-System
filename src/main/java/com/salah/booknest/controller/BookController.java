package com.salah.booknest.controller;

import com.salah.booknest.model.Book;
import com.salah.booknest.model.request.CreateBookRequest;
import com.salah.booknest.model.response.BookResponse;
import com.salah.booknest.service.BookService;
import jakarta.validation.Valid;
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
    public List<BookResponse> getBooks(){
        return bookService.getBooks();
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
