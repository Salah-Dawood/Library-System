package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Genre;
import com.salah.booknest.model.User;
import com.salah.booknest.repository.GenreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public ResponseEntity<?> updateGenre(Long genreId,Genre genreObject){
        if (genreObject.getName() !=null) {
            genreRepository.findByName(genreObject.getName())
                    .ifPresent(existingGenre -> {
                        throw new InformationExistException("Genre with name " + genreObject.getName() + " already exists");
                    });
        }

        Genre updatedGenre = genreRepository.getById(genreId);

        if (genreObject.getName() != null){
            updatedGenre.setName(genreObject.getName());
        }
        if (genreObject.getDescription()!=null){
            updatedGenre.setDescription(genreObject.getDescription());
        }
        genreRepository.save(updatedGenre);
        return new ResponseEntity<> (updatedGenre, HttpStatus.OK);

    }

    public ResponseEntity<?> deleteGenre(Long genreId){
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new InformationNotFoundException("genre with id " + genreId + " not found"));
        genreRepository.deleteById(genreId);
        return new ResponseEntity<> (HttpStatus.NO_CONTENT);
    }
}
