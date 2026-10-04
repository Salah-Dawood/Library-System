package com.salah.booknest.controller;

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

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @GetMapping
    @PreAuthorize("hasRole('librarian')")
    public List<LoanResponse> getLoans(@RequestParam(required = false) LoanStatus status) {
        return loanService.getLoans(status);
    }

    @GetMapping("/my-loans")
    public List<LoanResponse> getMyLoans(Authentication authentication) {
        return loanService.getMyLoans(authentication);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('librarian')")
    public List<LoanResponse> getByUserId(@PathVariable Long userId) {
        return loanService.getByUserId(userId);
    }

    @GetMapping("/my-stats")
    public ReturnStats getMyStats(Authentication authentication) {
        return loanService.getMyStats(authentication);
    }

    @GetMapping("/user/{userId}/stats")
    @PreAuthorize("hasRole('librarian')")
    public ReturnStats getUserStats(@PathVariable Long userId) {
        return loanService.getStats(userId);
    }

    @PostMapping
    public ResponseEntity<LoanResponse> requestLoan(Authentication authentication,
                                                    @Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.requestLoan(authentication, request));
    }

    @PutMapping("/{loanId}/approve")
    @PreAuthorize("hasRole('librarian')")
    public LoanResponse approve(@PathVariable Long loanId, Authentication authentication) {
        return loanService.approve(loanId, authentication);
    }

    @PutMapping("/{loanId}/reject")
    @PreAuthorize("hasRole('librarian')")
    public LoanResponse reject(@PathVariable Long loanId,
                               @RequestBody(required = false) RejectLoanRequest request,
                               Authentication authentication) {
        return loanService.reject(loanId, request == null ? null : request.reason(), authentication);
    }

    @PutMapping("/{loanId}/cancel")
    public LoanResponse cancel(@PathVariable Long loanId, Authentication authentication) {
        return loanService.cancel(loanId, authentication);
    }

    @PutMapping("/{loanId}/return")
    public LoanResponse returnLoan(@PathVariable Long loanId, Authentication authentication) {
        return loanService.returnLoan(loanId, authentication);
    }
}
