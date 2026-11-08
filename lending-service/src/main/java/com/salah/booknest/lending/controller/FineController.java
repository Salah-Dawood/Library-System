package com.salah.booknest.lending.controller;

import com.salah.booknest.lending.model.response.FineResponse;
import com.salah.booknest.lending.service.FineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Fines", description = "Fines for late and damaged returns.")
@RestController
@RequestMapping("/api/fines")
public class FineController {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @Operation(summary = "List all fines (paged)", description = "Every fine of every member, newest first by default. Sort by id, createdAt, amount, status or type. Page size is capped at 100. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown sortBy field")
    })
    @PreAuthorize("hasRole('librarian')")
    @GetMapping("")
    public Page<FineResponse> getAllFines(
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Fines per page (1 to 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id, createdAt, amount, status or type") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "desc") String sortDir){
        return fineService.getAllFines(page, size, sortBy, sortDir);
    }

    @Operation(summary = "List the fines of a member (paged)", description = "Fines of the member with this user id (empty page if there are none). Same paging and sorting as the full list. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown sortBy field")
    })
    @PreAuthorize("hasRole('librarian')")
    @GetMapping("/user/{id}")
    public Page<FineResponse> getUserFine(
            @Parameter(description = "Id of the user") @PathVariable Long id,
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Fines per page (1 to 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id, createdAt, amount, status or type") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "desc") String sortDir){
        return fineService.getUserFines(id, page, size, sortBy, sortDir);
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



    @Operation(summary = "List my fines (paged)", description = "The fines of the logged-in member, paid and unpaid, newest first by default. Same paging and sorting as the full list.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown sortBy field")
    })
    @GetMapping("/my-fines")
    public Page<FineResponse> getMyFines(
            Authentication authentication,
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Fines per page (1 to 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id, createdAt, amount, status or type") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "desc") String sortDir){
        return fineService.getMyFines(authentication, page, size, sortBy, sortDir);
    }
}
