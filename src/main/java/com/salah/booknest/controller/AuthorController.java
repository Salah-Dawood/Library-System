package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.model.Author;
import com.salah.booknest.service.AuthorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Authors", description = "Browse authors. Librarians create, edit and delete them.")
@RestController
@RequestMapping("/api/authors")
public class AuthorController {
    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @Operation(summary = "List authors", description = "Every author, with their books.")
    @GetMapping("")
    public List<Author> getAuthors(){
        return authorService.getAuthors();
    }

    @Operation(summary = "Get an author by id", description = "One author with their books.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No author with this id")
    })
    @GetMapping("{authorId}")
    public Author getAuthorById(@Parameter(description = "Id of the author") @PathVariable Long authorId){
        return authorService.getAuthorById(authorId);
    }

    @Operation(summary = "Find an author by name", description = "Exact name match.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No author with this name")
    })
    @GetMapping("/search/{name}")
    public Author getAuthorByName(@Parameter(description = "Exact name (case-sensitive)") @PathVariable String name){
        return authorService.getAuthorByName(name);
    }

    @Operation(summary = "Create an author", description = "Author names must be unique. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Author created", content = @Content(schema = @Schema(implementation = Author.class))),
            @ApiResponse(responseCode = "409", description = "An author with this name already exists")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"name\":\"Frank Herbert\",\"birthYear\":1920,\"nationality\":\"American\"}")))
    @PostMapping("")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> createAuthor(@RequestBody Author authorObject){
        return authorService.createAuthor(authorObject);
    }

    @Operation(summary = "Edit an author", description = "Only the fields you send are changed. An author can keep their own name. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Author updated", content = @Content(schema = @Schema(implementation = Author.class))),
            @ApiResponse(responseCode = "404", description = "No author with this id"),
            @ApiResponse(responseCode = "409", description = "Another author already has this name")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"name\":\"Frank Herbert\",\"birthYear\":1920,\"nationality\":\"American\"}")))
    @PutMapping("/{authorId}")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> updateAuthor(@Parameter(description = "Id of the author") @PathVariable Long authorId,
                                          @RequestBody Author authorObject){
        return authorService.updateAuthor(authorId,authorObject);
    }

    @Operation(summary = "Delete an author", description = "Also deletes all of the author's books and their loans and reviews. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Author deleted"),
            @ApiResponse(responseCode = "404", description = "No author with this id")
    })
    @DeleteMapping("/{authorId}")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> deleteAuthor(@Parameter(description = "Id of the author") @PathVariable Long authorId){
        return authorService.deleteAuthor(authorId);
    }

}
