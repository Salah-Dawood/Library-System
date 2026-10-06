package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.model.Genre;
import com.salah.booknest.service.GenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Genres", description = "Browse genres. Librarians create, edit and delete them.")
@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @Operation(summary = "List genres", description = "Every genre, with its books.")
    @GetMapping("")
    public List<Genre> getGenres(){
        return genreService.getGenres();
    }

    @Operation(summary = "Get a genre by name", description = "Exact name match.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No genre with this name")
    })
    @GetMapping("{name}")
    public Genre getGenre(@Parameter(description = "Exact name (case-sensitive)") @PathVariable String name){
        return genreService.getGenre(name);
    }

    @Operation(summary = "Create a genre", description = "Genre names must be unique. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Genre created", content = @Content(schema = @Schema(implementation = Genre.class))),
            @ApiResponse(responseCode = "409", description = "A genre with this name already exists")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"name\":\"Science Fiction\",\"description\":\"Speculative fiction about science and the future\"}")))
    @PostMapping("")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> createGenre(@RequestBody Genre genreObject){
        return genreService.createGenre(genreObject);
    }

    @Operation(summary = "Delete a genre", description = "Refused while any book still uses the genre. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Genre deleted"),
            @ApiResponse(responseCode = "404", description = "No genre with this id"),
            @ApiResponse(responseCode = "409", description = "The genre is still used by books")
    })
    @DeleteMapping("/{genreId}")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> deleteGenre(@Parameter(description = "Id of the genre") @PathVariable Long genreId){
        return genreService.deleteGenre(genreId);
    }

    @Operation(summary = "Edit a genre", description = "Only the fields you send are changed. A genre can keep its own name. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Genre updated", content = @Content(schema = @Schema(implementation = Genre.class))),
            @ApiResponse(responseCode = "404", description = "No genre with this id"),
            @ApiResponse(responseCode = "409", description = "Another genre already has this name")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"name\":\"Science Fiction\",\"description\":\"Speculative fiction about science and the future\"}")))
    @PutMapping("/{genreId}")
    @PreAuthorize("hasRole('librarian')")
    public ResponseEntity<?> updateGenre(@Parameter(description = "Id of the genre") @PathVariable Long genreId,
                                         @RequestBody Genre genreObject){
        return genreService.updateGenre(genreId,genreObject);
    }
}
