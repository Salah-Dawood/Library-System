package com.salah.booknest.controller;

import com.salah.booknest.model.response.FineResponse;
import com.salah.booknest.service.FineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Fines", description = "Fines for late and damaged returns.")
@RestController
@RequestMapping("/api/fines")
public class FineController {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @Operation(summary = "List all fines", description = "Every fine of every member. Librarian only.")
    @PreAuthorize("hasRole('librarian')")
    @GetMapping("")
    public ResponseEntity<List<FineResponse>> getAllFines(){
        return new ResponseEntity<>(fineService.getAllFines(),HttpStatus.OK);
    }

    @Operation(summary = "List the fines of a member", description = "All fines of the member with this user id (empty list if there are none). Librarian only.")
    @PreAuthorize("hasRole('librarian')")
    @GetMapping("/user/{id}")
    public ResponseEntity<List<FineResponse>> getUserFine(@PathVariable Long id){
        return new ResponseEntity<>(fineService.getUserFines(id),HttpStatus.OK);
    }

    @Operation(summary = "Pay a fine", description = "Marks one of your own fines as paid. The fine is locked while it is paid, so paying the same fine twice at the same moment only works once.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fine paid"),
            @ApiResponse(responseCode = "403", description = "Not your fine"),
            @ApiResponse(responseCode = "404", description = "No fine with this id"),
            @ApiResponse(responseCode = "409", description = "FINE_ALREADY_PAID, or CONCURRENT_UPDATE (try again)")
    })
    @PutMapping("/{id}/pay")
    public ResponseEntity<FineResponse> payFine(@PathVariable Long id,Authentication authentication){
        return new ResponseEntity<>(fineService.payFine(id,authentication),HttpStatus.OK);
    }



    @Operation(summary = "List my fines", description = "The fines of the logged-in member, paid and unpaid.")
    @GetMapping("/my-fines")
    public ResponseEntity<List<FineResponse>> getMyFines(Authentication authentication){
        return new ResponseEntity<>(fineService.getMyFines(authentication),HttpStatus.OK);
    }
}
