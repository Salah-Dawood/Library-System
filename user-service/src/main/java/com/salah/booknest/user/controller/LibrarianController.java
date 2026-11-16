package com.salah.booknest.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.user.model.request.RegisterRequest;
import com.salah.booknest.user.model.response.LoginResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.salah.booknest.user.model.response.UserDetailResponse;
import com.salah.booknest.user.service.LibrarianService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User administration", description = "Librarian only: create librarians, list, inspect, activate, deactivate and delete users.")
@RestController
@RequestMapping("/api/librarian")
@PreAuthorize("hasRole('librarian')")
@RequiredArgsConstructor
public class LibrarianController {

    private final LibrarianService librarianService;

    @Operation(summary = "Create a librarian", description = "Creates a new librarian account with the starting password you choose. It is verified at once, so the new librarian can log in straight away. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Librarian created"),
            @ApiResponse(responseCode = "400", description = "Invalid details"),
            @ApiResponse(responseCode = "409", description = "Username or email already registered")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"username\":\"sara\",\"emailAddress\":\"sara@example.com\",\"password\":\"Passw0rd!\",\"firstName\":\"Sara\",\"lastName\":\"Ali\",\"age\":30}")))
    @PostMapping("/users")
    public ResponseEntity<UserDetailResponse> createLibrarian(@Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(librarianService.createLibrarian(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Get a user with their profile", description = "Account details plus profile (name, age, bio, picture). Never includes the password or verification code. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No user with this id")
    })
    @GetMapping("/users/{userId}")
    public UserDetailResponse getUser(@Parameter(description = "Id of the user") @PathVariable Long userId) {
        return librarianService.getUserDetail(userId);
    }

    @Operation(summary = "List users (paged)", description = "Summary of every account, ordered by id by default. Sort by id, username, emailAddress, role or createdAt. Page size is capped at 100. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown sortBy field")
    })
    @GetMapping("/users")
    public Page<LoginResponse.UserSummary> getUsers(
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Users per page (1 to 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id, username, emailAddress, role or createdAt") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "asc") String sortDir) {
        return librarianService.getUsers(page, size, sortBy, sortDir);
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
