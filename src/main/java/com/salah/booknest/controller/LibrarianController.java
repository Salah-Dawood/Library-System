package com.salah.booknest.controller;

import com.salah.booknest.model.User;
import com.salah.booknest.model.response.LoginResponse;
import com.salah.booknest.service.LibrarianService;
import com.salah.booknest.service.UserService;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/librarian")
@PreAuthorize("hasRole('librarian')")
public class LibrarianController {
    private final LibrarianService librarianService;

    public LibrarianController(LibrarianService librarianService) {
        this.librarianService = librarianService;
    }

    @GetMapping("/users")
    public List<LoginResponse.UserSummary> getUsers() { return librarianService.getUsers(); }


    @DeleteMapping("/users/delete/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId){
        System.out.println("calling deleteUser()");
        librarianService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/deactivate/{userId}")
    public User deactivateUser(@PathVariable Long userId){
        System.out.println("calling softDeleteUser()");
        return librarianService.deactivateUser(userId);
    }

    @PutMapping("/users/activate/{userId}")
    public User activateUser(@PathVariable Long userId){
        System.out.println("calling softDeleteUser()");
        return librarianService.activateUser(userId);
    }
}
