package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.Genre;
import com.salah.booknest.model.request.GenreRequest;
import com.salah.booknest.model.response.GenreResponse;
import com.salah.booknest.repository.GenreRepository;
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
    public List<GenreResponse> getGenres(){
        return genreRepository.findAll().stream()
                .map(GenreResponse::from)
                .toList();
    }

    //return genre
    public GenreResponse getGenre(String name){
        Genre genre = genreRepository.findByName(name)
                .orElseThrow(() -> new InformationNotFoundException("genre with name " + name + " not found"));
        return GenreResponse.from(genre);
    }

    //create genre
    public GenreResponse createGenre(GenreRequest request){
        genreRepository.findByName(request.getName())
                .ifPresent(existingGenre -> {
                    throw new InformationExistException("Genre with name " + request.getName() + " already exists");
                });
        Genre genre = new Genre();

        genre.setName(request.getName());
        genre.setDescription(request.getDescription());
        Genre savedGenre = genreRepository.save(genre);
        return GenreResponse.from(savedGenre);
    }

    public GenreResponse updateGenre(Long genreId, GenreRequest request) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new InformationNotFoundException("Genre with ID " + genreId + " not found"));
        if (request.getName() != null) {
            genreRepository.findByName(request.getName())
                    .filter(existing -> !existing.getId().equals(genreId))
                    .ifPresent(existing -> {
                        throw new InformationExistException("Genre with name " + request.getName() + " already exists");
                    });
            genre.setName(request.getName());
        }
        if (request.getDescription() != null) {
            genre.setDescription(request.getDescription());
        }
        return GenreResponse.from(genreRepository.save(genre));
    }

    @Transactional
    public void deleteGenre(Long genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new InformationNotFoundException("Genre with ID " + genreId + " not found"));
        if (!genre.getBooks().isEmpty()) {
            throw new InvalidStateException(
                    "Genre \"" + genre.getName() + "\" is still used by " + genre.getBooks().size() + " book(s)");
        }
        genreRepository.delete(genre);
    }
}
