package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Author;
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

    public ResponseEntity<?> createAuthor(Author authorObject){
        if (authorRepository.existsByName(authorObject.getName())){
            throw new InformationExistException("Author with name " + authorObject.getName() + " already exists");
        }
        authorRepository.save(authorObject);
        return new ResponseEntity<>(authorObject, HttpStatus.CREATED);
    }

    /** Edits only the fields that are sent. The name must stay unique, but an author may keep their own. */
    public ResponseEntity<?> updateAuthor(Long authorId, Author authorObject) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new InformationNotFoundException("Author with ID " + authorId + " does not exist"));
        if (authorObject.getName() != null) {
            authorRepository.findByName(authorObject.getName())
                    .filter(existing -> !existing.getId().equals(authorId))
                    .ifPresent(existing -> {
                        throw new InformationExistException("Author with name " + authorObject.getName() + " already exists");
                    });
            author.setName(authorObject.getName());
        }
        if (authorObject.getBirthYear() != null) {
            author.setBirthYear(authorObject.getBirthYear());
        }
        if (authorObject.getNationality() != null) {
            author.setNationality(authorObject.getNationality());
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
