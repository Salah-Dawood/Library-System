package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.model.LoanStatus;
import com.salah.booknest.model.request.LoanRequest;
import com.salah.booknest.model.request.ReviewReturnRequest;
import com.salah.booknest.model.response.LoanResponse;
import com.salah.booknest.model.response.ReturnStats;
import com.salah.booknest.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Loans", description = "Members borrow books instantly and return them; librarians review returned books for damage.")
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @Operation(summary = "List all loans (paged)", description = "Newest first by default. Filter with ?status=BORROWED, PENDING_REVIEW, RETURNED or CANCELLED. Sort by id, createdAt, loanDate, dueDate, returnDate or status. Page size is capped at 100. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown status or sortBy field")
    })
    @GetMapping
    @PreAuthorize("hasRole('librarian')")
    public Page<LoanResponse> getLoans(
            @Parameter(description = "BORROWED, PENDING_REVIEW, RETURNED or CANCELLED") @RequestParam(required = false) LoanStatus status,
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Loans per page (1 to 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id, createdAt, loanDate, dueDate, returnDate or status") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "desc") String sortDir) {
        return loanService.getLoans(status, page, size, sortBy, sortDir);
    }

    @Operation(summary = "Returns review queue (paged)", description = "Returned books waiting for a librarian to check them for damage, oldest return first by default. Each entry shows return timing (early, on time, late) and days late. Same paging and sorting as the full list. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown sortBy field")
    })
    @GetMapping("/returns-review")
    @PreAuthorize("hasRole('librarian')")
    public Page<LoanResponse> getReturnsReview(
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Loans per page (1 to 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id, createdAt, loanDate, dueDate, returnDate or status") @RequestParam(defaultValue = "returnDate") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "asc") String sortDir) {
        return loanService.getReturnsReview(page, size, sortBy, sortDir);
    }

    @Operation(summary = "List my loans (paged)", description = "Loans of the logged-in user, newest first by default. Includes return timing (early, on time, late) and an overdue flag. Same paging and sorting as the full list.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown sortBy field")
    })
    @GetMapping("/my-loans")
    public Page<LoanResponse> getMyLoans(
            Authentication authentication,
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Loans per page (1 to 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id, createdAt, loanDate, dueDate, returnDate or status") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "desc") String sortDir) {
        return loanService.getMyLoans(authentication, page, size, sortBy, sortDir);
    }

    @Operation(summary = "List a user's loans (paged)", description = "Newest first by default. Same paging and sorting as the full list. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown sortBy field")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('librarian')")
    public Page<LoanResponse> getByUserId(
            @Parameter(description = "Id of the user") @PathVariable Long userId,
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Loans per page (1 to 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id, createdAt, loanDate, dueDate, returnDate or status") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "desc") String sortDir) {
        return loanService.getByUserId(userId, page, size, sortBy, sortDir);
    }

    @Operation(summary = "My return history", description = "Counts of early, on-time and late returns, and the Reliable member badge (at least 3 returns, 90% on time or early).")
    @GetMapping("/my-stats")
    public ReturnStats getMyStats(Authentication authentication) {
        return loanService.getMyStats(authentication);
    }

    @Operation(summary = "A user's return history", description = "Same figures as /my-stats, for any user. Librarian only.")
    @GetMapping("/user/{userId}/stats")
    @PreAuthorize("hasRole('librarian')")
    public ReturnStats getUserStats(@Parameter(description = "Id of the user") @PathVariable Long userId) {
        return loanService.getStats(userId);
    }

    @Operation(summary = "Borrow a book", description = "Borrowing is instant, no approval needed: one copy is taken from the stock and the loan and due dates are set. Limits: one loan per book and at most 5 books on loan.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book borrowed", content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "404", description = "No book with this id"),
            @ApiResponse(responseCode = "409", description = "NO_COPIES_AVAILABLE, LOAN_LIMIT_REACHED (5 books), RESOURCE_ALREADY_EXISTS (you already have this book), or CONCURRENT_UPDATE (another request changed the same data at the same moment, try again)")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"bookId\":1,\"duration\":14}")))
    @PostMapping
    public ResponseEntity<LoanResponse> borrow(Authentication authentication,
                                               @Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrow(authentication, request));
    }

    @Operation(summary = "Review a returned book", description = "The librarian checks the returned copy. Not damaged: the copy goes back in stock. Damaged: the copy is taken out of circulation. An optional fine can be added either way. The member is notified. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid fine amount or notes too long"),
            @ApiResponse(responseCode = "404", description = "No loan with this id"),
            @ApiResponse(responseCode = "409", description = "The loan is not waiting for review")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"damaged\":true,\"notes\":\"Water damage on the cover\",\"fineAmount\":5.00}")))
    @PutMapping("/{loanId}/review-return")
    @PreAuthorize("hasRole('librarian')")
    public LoanResponse reviewReturn(@Parameter(description = "Id of the loan") @PathVariable Long loanId,
                                     @Valid @RequestBody ReviewReturnRequest request,
                                     Authentication authentication) {
        return loanService.reviewReturn(loanId, request, authentication);
    }

    @Operation(summary = "Cancel a loan", description = "Cancels a loan that is still out and gives the copy back to the stock. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No loan with this id"),
            @ApiResponse(responseCode = "409", description = "The loan cannot be cancelled in its current status")
    })
    @PutMapping("/{loanId}/cancel")
    @PreAuthorize("hasRole('librarian')")
    public LoanResponse cancel(@Parameter(description = "Id of the loan") @PathVariable Long loanId) {
        return loanService.cancel(loanId);
    }

    @Operation(summary = "Return a book", description = "The borrower returns their own loan, or a librarian records the return. The return is judged against the due date (early, on time, late) and the loan goes to PENDING_REVIEW so a librarian can check the copy for damage. The copy goes back to the stock once the librarian clears it.")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = "Not your loan"),
            @ApiResponse(responseCode = "404", description = "No loan with this id"),
            @ApiResponse(responseCode = "409", description = "The loan is not currently borrowed")
    })
    @PutMapping("/{loanId}/return")
    public LoanResponse returnLoan(@Parameter(description = "Id of the loan") @PathVariable Long loanId, Authentication authentication) {
        return loanService.returnLoan(loanId, authentication);
    }
}
