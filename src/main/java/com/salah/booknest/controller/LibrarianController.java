package com.salah.booknest.controller;

import com.salah.booknest.model.response.LoginResponse;
import com.salah.booknest.service.LibrarianService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/librarian")
@PreAuthorize("hasRole('librarian')")
@RequiredArgsConstructor
public class LibrarianController {

    private final LibrarianService librarianService;

    @GetMapping("/users")
    public List<LoginResponse.UserSummary> getUsers() {
        return librarianService.getUsers();
    }

    @DeleteMapping("/users/delete/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId, Authentication authentication) {
        librarianService.deleteUser(userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/deactivate/{userId}")
    public LoginResponse.UserSummary deactivateUser(@PathVariable Long userId, Authentication authentication) {
        return librarianService.deactivateUser(userId, authentication.getName());
    }

    @PutMapping("/users/activate/{userId}")
    public LoginResponse.UserSummary activateUser(@PathVariable Long userId, Authentication authentication) {
        return librarianService.activateUser(userId, authentication.getName());
    }
}
