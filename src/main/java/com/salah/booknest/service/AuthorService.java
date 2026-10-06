package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Author;
import com.salah.booknest.model.request.AuthorRequest;
import com.salah.booknest.repository.AuthorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<Author> getAuthors(){
        return authorRepository.findAll();
    }

    public Author getAuthorById(Long authorId){
        return authorRepository.findById(authorId)
                .orElseThrow(() -> new InformationNotFoundException("Author with ID " + authorId + " is does not exist"));
    }

    public Author getAuthorByName(String name){
        return authorRepository.findByName(name)
                .orElseThrow(() -> new InformationNotFoundException("Author with name " + name + " is does not exist"));
    }

    public ResponseEntity<?> createAuthor(AuthorRequest request){
        if (authorRepository.existsByName(request.getName())){
            throw new InformationExistException("Author with name " + request.getName() + " already exists");
        }
        Author newAuthor = new Author();
        newAuthor.setName(request.getName());
        newAuthor.setBirthYear(request.getBirthYear());
        newAuthor.setNationality(request.getNationality());

        authorRepository.save(newAuthor);
        return new ResponseEntity<>(request, HttpStatus.CREATED);
    }

    public ResponseEntity<?> updateAuthor(Long authorId, AuthorRequest request) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new InformationNotFoundException("Author with ID " + authorId + " does not exist"));
        if (request.getName() != null) {
            authorRepository.findByName(request.getName())
                    .filter(existing -> !existing.getId().equals(authorId))
                    .ifPresent(existing -> {
                        throw new InformationExistException("Author with name " + request.getName() + " already exists");
                    });
            author.setName(request.getName());
        }
        if (request.getBirthYear() != null) {
            author.setBirthYear(request.getBirthYear());
        }
        if (request.getNationality() != null) {
            author.setNationality(request.getNationality());
        }
        return new ResponseEntity<>(authorRepository.save(author), HttpStatus.OK);
    }

    public ResponseEntity<?> deleteAuthor(Long authorId) {
        if (!authorRepository.existsById(authorId)) {
            throw new InformationNotFoundException("Author with ID " + authorId + " does not exist");
        }
        authorRepository.deleteById(authorId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
