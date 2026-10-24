package com.salah.booknest.controller;

import com.salah.booknest.model.request.WaitlistRequest;
import com.salah.booknest.model.response.WaitlistResponse;
import com.salah.booknest.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Waitlist", description = "Members wait in line for a book that has no free copies.")
@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @Operation(summary = "Join the waitlist for a book", description = "Only works when the book has no free copies. You can wait for at most 5 books and once per book. The position is your place in line, 1 means next.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Joined the waitlist"),
            @ApiResponse(responseCode = "404", description = "No book with this id"),
            @ApiResponse(responseCode = "409", description = "Copies are available, you already wait for or have this book, or you wait for 5 books already")
    })
    @PostMapping
    public ResponseEntity<WaitlistResponse> join(Authentication authentication,
                                                 @Valid @RequestBody WaitlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(waitlistService.join(authentication, request));
    }
}
