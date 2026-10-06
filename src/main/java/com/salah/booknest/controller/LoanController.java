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
import com.salah.booknest.model.request.RejectLoanRequest;
import com.salah.booknest.model.response.LoanResponse;
import com.salah.booknest.model.response.ReturnStats;
import com.salah.booknest.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Loans", description = "Members request books and return them; librarians approve or reject requests.")
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @Operation(summary = "List all loans", description = "Newest first. Filter with ?status=REQUESTED, APPROVED, REJECTED, CANCELLED or RETURNED. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown status")
    })
    @GetMapping
    @PreAuthorize("hasRole('librarian')")
    public List<LoanResponse> getLoans(@Parameter(description = "REQUESTED, APPROVED, REJECTED, CANCELLED or RETURNED") @RequestParam(required = false) LoanStatus status) {
        return loanService.getLoans(status);
    }

    @Operation(summary = "List my loans", description = "Loans of the logged-in user, newest first. Includes return timing (early, on time, late) and an overdue flag.")
    @GetMapping("/my-loans")
    public List<LoanResponse> getMyLoans(Authentication authentication) {
        return loanService.getMyLoans(authentication);
    }

    @Operation(summary = "List a user's loans", description = "Newest first. Librarian only.")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('librarian')")
    public List<LoanResponse> getByUserId(@Parameter(description = "Id of the user") @PathVariable Long userId) {
        return loanService.getByUserId(userId);
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

    @Operation(summary = "Request a loan", description = "Creates a REQUESTED loan. No copy is taken until a librarian approves. Limits: one active request per book and at most 5 active requests or loans.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan requested", content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "404", description = "No book with this id"),
            @ApiResponse(responseCode = "409", description = "Already requested this book, or the active-loan limit is reached")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"bookId\":1,\"duration\":14}")))
    @PostMapping
    public ResponseEntity<LoanResponse> requestLoan(Authentication authentication,
                                                    @Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.requestLoan(authentication, request));
    }

    @Operation(summary = "Approve a loan request", description = "Takes one copy from the stock and sets the loan and due dates. Row locks stop two approvals taking the last copy. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No loan with this id"),
            @ApiResponse(responseCode = "409", description = "The loan is not in REQUESTED status, or no copies are available")
    })
    @PutMapping("/{loanId}/approve")
    @PreAuthorize("hasRole('librarian')")
    public LoanResponse approve(@Parameter(description = "Id of the loan") @PathVariable Long loanId, Authentication authentication) {
        return loanService.approve(loanId, authentication);
    }

    @Operation(summary = "Reject a loan request", description = "Optional reason, shown to the member. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No loan with this id"),
            @ApiResponse(responseCode = "409", description = "The loan is not in REQUESTED status")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"reason\":\"The book is reserved for a class\"}")))
    @PutMapping("/{loanId}/reject")
    @PreAuthorize("hasRole('librarian')")
    public LoanResponse reject(@Parameter(description = "Id of the loan") @PathVariable Long loanId,
                               @RequestBody(required = false) RejectLoanRequest request,
                               Authentication authentication) {
        return loanService.reject(loanId, request == null ? null : request.reason(), authentication);
    }

    @Operation(summary = "Cancel a loan", description = "Members can cancel their own REQUESTED loans. Librarians can also cancel APPROVED loans, which gives the copy back.")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = "Not your loan, or only a librarian can cancel an approved loan"),
            @ApiResponse(responseCode = "404", description = "No loan with this id"),
            @ApiResponse(responseCode = "409", description = "The loan cannot be cancelled in its current status")
    })
    @PutMapping("/{loanId}/cancel")
    public LoanResponse cancel(@Parameter(description = "Id of the loan") @PathVariable Long loanId, Authentication authentication) {
        return loanService.cancel(loanId, authentication);
    }

    @Operation(summary = "Return a book", description = "The borrower returns their own APPROVED loan, or a librarian records the return. The copy goes back to the stock and the return is judged against the due date.")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = "Not your loan"),
            @ApiResponse(responseCode = "404", description = "No loan with this id"),
            @ApiResponse(responseCode = "409", description = "The loan is not APPROVED")
    })
    @PutMapping("/{loanId}/return")
    public LoanResponse returnLoan(@Parameter(description = "Id of the loan") @PathVariable Long loanId, Authentication authentication) {
        return loanService.returnLoan(loanId, authentication);
    }
}
