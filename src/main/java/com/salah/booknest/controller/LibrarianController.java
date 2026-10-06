package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.model.response.LoginResponse;
import com.salah.booknest.model.response.UserDetailResponse;
import com.salah.booknest.service.LibrarianService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User administration", description = "Librarian only: list, inspect, activate, deactivate and delete users.")
@RestController
@RequestMapping("/api/librarian")
@PreAuthorize("hasRole('librarian')")
@RequiredArgsConstructor
public class LibrarianController {

    private final LibrarianService librarianService;

    @Operation(summary = "Get a user with their profile", description = "Account details plus profile (name, age, bio, picture). Never includes the password or verification code. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No user with this id")
    })
    @GetMapping("/users/{userId}")
    public UserDetailResponse getUser(@Parameter(description = "Id of the user") @PathVariable Long userId) {
        return librarianService.getUserDetail(userId);
    }

    @Operation(summary = "List users", description = "Summary of every account. Librarian only.")
    @GetMapping("/users")
    public List<LoginResponse.UserSummary> getUsers() {
        return librarianService.getUsers();
    }

    @Operation(summary = "Delete a user", description = "Permanently removes the user. A librarian cannot delete their own account. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "No user with this id"),
            @ApiResponse(responseCode = "409", description = "You cannot change your own account")
    })
    @DeleteMapping("/users/delete/{userId}")
    public ResponseEntity<Void> deleteUser(@Parameter(description = "Id of the user") @PathVariable Long userId, Authentication authentication) {
        librarianService.deleteUser(userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate a user", description = "The user can no longer log in, and existing tokens stop working. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No user with this id"),
            @ApiResponse(responseCode = "409", description = "You cannot change your own account")
    })
    @PutMapping("/users/deactivate/{userId}")
    public LoginResponse.UserSummary deactivateUser(@Parameter(description = "Id of the user") @PathVariable Long userId, Authentication authentication) {
        return librarianService.deactivateUser(userId, authentication.getName());
    }

    @Operation(summary = "Activate a user", description = "Lets a deactivated user log in again. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No user with this id"),
            @ApiResponse(responseCode = "409", description = "You cannot change your own account")
    })
    @PutMapping("/users/activate/{userId}")
    public LoginResponse.UserSummary activateUser(@Parameter(description = "Id of the user") @PathVariable Long userId, Authentication authentication) {
        return librarianService.activateUser(userId, authentication.getName());
    }
}
