package com.salah.booknest.controller;

import com.salah.booknest.model.Author;
import com.salah.booknest.service.AuthorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {
    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping("")
    public List<Author> getAuthors(){
        return authorService.getAuthors();
    }

    @GetMapping("{authorId}")
    public Author getAuthorById(@PathVariable Long authorId){
        return authorService.getAuthorById(authorId);
    }

    @GetMapping("/search/{name}")
    public Author getAuthorByName(@PathVariable String name){
        return authorService.getAuthorByName(name);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> createAuthor(@RequestBody Author authorObject){
        return authorService.createAuthor(authorObject);
    }

}
