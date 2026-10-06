package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.model.request.CreateBookRequest;
import com.salah.booknest.model.response.BookResponse;
import com.salah.booknest.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Books", description = "Browse, search and filter the catalogue (paged). Librarians manage books.")
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @Operation(summary = "List books (paged)", description = "Optional filters: title (contains, ignoring case) and genre (exact name, ignoring case). Sort by id, title, publishedYear or createdAt. Page size is capped at 100.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown sortBy field")
    })
    @GetMapping("")
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            // Optional Filtering Parameters
            @Parameter(description = "Part of the title, ignoring case") @RequestParam(required = false) String title,
            @Parameter(description = "Exact genre name, ignoring case") @RequestParam(required = false) String genre,

            // Pagination Parameters (Defaults: Page 0, 10 items per page)
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Books per page (1 to 100)") @RequestParam(defaultValue = "10") int size,

            // Sorting Parameters (Defaults: Sort by 'id' in 'asc' order)
            @Parameter(description = "id, title, publishedYear or createdAt") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<BookResponse> booksPage = bookService.getBooks(title, genre, page, size, sortBy, sortDir);
        return ResponseEntity.ok(booksPage);
    }

    @Operation(summary = "Get a book", description = "Details, stock and rating of one book.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No book with this id")
    })
    @GetMapping("/{bookId}")
    public BookResponse getBookById(@Parameter(description = "Id of the book") @PathVariable Long bookId){
        return bookService.getBookById(bookId);
    }

    @Operation(summary = "Find a book by exact title", description = "Exact title match.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No book with this title")
    })
    @GetMapping("/search/{title}")
    public BookResponse getBookByTitle(@Parameter(description = "Exact book title") @PathVariable String title){
        return bookService.getBookByTitle(title);
    }

    @Operation(summary = "Create a book", description = "Creates the book and its stock. The ISBN must be unique and at least one genre is required. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book created", content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "404", description = "Author or genre not found"),
            @ApiResponse(responseCode = "409", description = "A book with this ISBN already exists")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"title\":\"Dune\",\"isbn\":\"978-0441013593\",\"publishedYear\":1965,\"authorId\":1,\"totalCopies\":3,\"genreIds\":[1,2]}")))
    @PostMapping("")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> createBook(@Valid @RequestBody CreateBookRequest request){
        return bookService.createBook(request);
    }

    @Operation(summary = "Edit a book", description = "Only the fields you send are changed. Changing totalCopies keeps the number of copies on loan, and cannot go below it. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book updated", content = @Content(schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "404", description = "Book, author or genre not found"),
            @ApiResponse(responseCode = "409", description = "Another book already has this ISBN")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"title\":\"Dune\",\"isbn\":\"978-0441013593\",\"publishedYear\":1965,\"authorId\":1,\"totalCopies\":4,\"genreIds\":[1]}")))
    @PutMapping("/{bookId}")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity updateBook(@Parameter(description = "Id of the book") @PathVariable Long bookId,
                                     @Valid @RequestBody CreateBookRequest request){
        return bookService.updateBook(bookId,request);
    }

    @Operation(summary = "Delete a book", description = "Also deletes its loans and reviews. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Book deleted"),
            @ApiResponse(responseCode = "404", description = "No book with this id")
    })
    @DeleteMapping("/{bookId}")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> deleteBook(@Parameter(description = "Id of the book") @PathVariable Long bookId){
        return bookService.deleteBook(bookId);
    }
}