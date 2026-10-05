package com.salah.booknest.controller;

import com.salah.booknest.model.Genre;
import com.salah.booknest.service.GenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping("")
    public List<Genre> getGenres(){
        return genreService.getGenres();
    }

    @GetMapping("{name}")
    public Genre getGenre(@PathVariable String name){
        return genreService.getGenre(name);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> createGenre(@RequestBody Genre genreObject){
        return genreService.createGenre(genreObject);
    }

    @DeleteMapping("/{genreId}")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> deleteGenre(@PathVariable Long genreId){
        return genreService.deleteGenre(genreId);
    }

    @PutMapping("/{genreId}")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> updateGenre(@PathVariable Long genreId,
                                         @RequestBody Genre genreObject){
        return genreService.updateGenre(genreId,genreObject);
    }
}
