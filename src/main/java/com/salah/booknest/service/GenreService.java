package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Genre;
import com.salah.booknest.repository.GenreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }


    //return all genres
    public List<Genre> getGenres(){
        return genreRepository.findAll();
    }

    //return genre
    public Genre getGenre(String name){
        Genre genre = genreRepository.findByName(name)
                .orElseThrow(() -> new InformationNotFoundException("genre with name " + name + " not found"));
        return genre;
    }

    //create genre
    public ResponseEntity<?> createGenre(Genre genreObject){
        genreRepository.findByName(genreObject.getName())
                .ifPresent(existingGenre -> {
                    throw new InformationExistException("Genre with name " + genreObject.getName() + " already exists");
                });
        Genre genre = new Genre();

        genre.setName(genreObject.getName());
        genre.setDescription(genreObject.getDescription());
        genreRepository.save(genre);
        return new ResponseEntity<> (genre, HttpStatus.CREATED);
    }

    /** Edits only the fields that are sent. The name must stay unique, but a genre may keep its own. */
    public ResponseEntity<?> updateGenre(Long genreId, Genre genreObject) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new InformationNotFoundException("Genre with ID " + genreId + " not found"));
        if (genreObject.getName() != null) {
            genreRepository.findByName(genreObject.getName())
                    .filter(existing -> !existing.getId().equals(genreId))
                    .ifPresent(existing -> {
                        throw new InformationExistException("Genre with name " + genreObject.getName() + " already exists");
                    });
            genre.setName(genreObject.getName());
        }
        if (genreObject.getDescription() != null) {
            genre.setDescription(genreObject.getDescription());
        }
        return new ResponseEntity<>(genreRepository.save(genre), HttpStatus.OK);
    }

    /** A genre that books still use cannot be deleted; remove it from those books first. */
    @Transactional
    public ResponseEntity<?> deleteGenre(Long genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new InformationNotFoundException("Genre with ID " + genreId + " not found"));
        if (!genre.getBooks().isEmpty()) {
            throw new InvalidStateException(
                    "Genre \"" + genre.getName() + "\" is still used by " + genre.getBooks().size() + " book(s)");
        }
        genreRepository.delete(genre);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
