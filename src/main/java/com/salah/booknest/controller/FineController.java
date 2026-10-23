package com.salah.booknest.controller;

import com.salah.booknest.model.response.FineResponse;
import com.salah.booknest.service.FineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
public class FineController {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @PreAuthorize("hasRole('librarian')")
    @GetMapping("")
    public ResponseEntity<List<FineResponse>> getAllFines(){
        return new ResponseEntity<>(fineService.getAllFines(),HttpStatus.OK);
    }

    @PreAuthorize("hasRole('librarian')")
    @GetMapping("/user/{id}")
    public ResponseEntity<List<FineResponse>> getUserFine(@PathVariable Long id){
        return new ResponseEntity<>(fineService.getUserFines(id),HttpStatus.OK);
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<FineResponse> payFine(@PathVariable Long id,Authentication authentication){
        return new ResponseEntity<>(fineService.payFine(id,authentication),HttpStatus.OK);
    }



    @GetMapping("/my-fines")
    public ResponseEntity<List<FineResponse>> getMyFines(Authentication authentication){
        return new ResponseEntity<>(fineService.getMyFines(authentication),HttpStatus.OK);
    }
}
