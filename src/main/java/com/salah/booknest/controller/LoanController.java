package com.salah.booknest.controller;

import com.salah.booknest.model.Loan;
import com.salah.booknest.model.request.LoanRequest;
import com.salah.booknest.model.response.LoanResonse;
import com.salah.booknest.service.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("")
    @PreAuthorize("hasRole('librarian')")
    public List<LoanResonse> getLoans(){
        return loanService.getLoans();
    }

    @GetMapping("/my-loans")
    public List<LoanResonse> getMyLoans(Authentication authentication){
        return loanService.getMyLoans(authentication);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('librarian')")
    public List<LoanResonse> getByUserId(@PathVariable Long userId){
        return loanService.getByUserId(userId);
    }

    @PostMapping("")
    public ResponseEntity<?> createLoan(Authentication authentication,
                                        @RequestBody LoanRequest request){
        return loanService.createLoan(authentication,request);
    }
}
