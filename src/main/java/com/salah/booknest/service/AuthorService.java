package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.Author;
import com.salah.booknest.repository.AuthorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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

    public ResponseEntity<?> updateAuthor(Long authorId,Author authorObject){
        if (authorRepository.existsByName(authorObject.getName())){
            throw new InformationExistException("Author with name " + authorObject.getName() + " already exists");
        }
        Author updatedAuthor = authorRepository.findById(authorId)
                .orElseThrow(() -> new InformationNotFoundException("Author with ID " + authorId + " is does not exist"));

        if (authorObject.getName() != null){
            updatedAuthor.setName(authorObject.getName());
        }
        if (authorObject.getBirthYear() != null){
            updatedAuthor.setBirthYear(authorObject.getBirthYear());
        }
        if (authorObject.getNationality() != null){
            updatedAuthor.setNationality(authorObject.getNationality());
        }
        authorRepository.save(updatedAuthor);
        return new ResponseEntity<>(authorObject, HttpStatus.OK);
    }

    public ResponseEntity<?> deleteAuthor(Long authorId){
        authorRepository.deleteById(authorId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);

    }
}
