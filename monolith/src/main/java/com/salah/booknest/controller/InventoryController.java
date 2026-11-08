package com.salah.booknest.controller;

import com.salah.booknest.model.request.InventoryRequest;
import com.salah.booknest.model.response.InventoryResponse;
import com.salah.booknest.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Inventory", description = "Copy counts of the books. Used by the catalog service.")
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Copy counts of several books", description = "Books without stock are left out.")
    @GetMapping("")
    public List<InventoryResponse> getStock(@Parameter(description = "Ids of the books, for example 1,2,3") @RequestParam List<Long> bookIds) {
        return inventoryService.getStock(bookIds);
    }

    @Operation(summary = "Create the stock of a new book", description = "All copies start on the shelf. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No book with this id"),
            @ApiResponse(responseCode = "409", description = "The book already has stock")
    })
    @PreAuthorize("hasRole('librarian')")
    @PostMapping("")
    public ResponseEntity<InventoryResponse> createStock(@Valid @RequestBody InventoryRequest request) {
        return new ResponseEntity<>(inventoryService.createStock(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Change the total copies of a book", description = "Copies on loan are kept. New copies go to the waitlist first. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "The new total is lower than the copies on loan"),
            @ApiResponse(responseCode = "404", description = "No stock for this book")
    })
    @PreAuthorize("hasRole('librarian')")
    @PutMapping("/{bookId}")
    public InventoryResponse changeTotalCopies(@PathVariable Long bookId, @Valid @RequestBody InventoryRequest request) {
        return inventoryService.changeTotalCopies(bookId, request.totalCopies());
    }

    @Operation(summary = "Take a book out of lending", description = "Cancels its waitlist. Refused while a copy is on loan or waiting for review. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "409", description = "The book is on loan or has returns waiting for review")
    })
    @PreAuthorize("hasRole('librarian')")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> retire(@PathVariable Long bookId) {
        inventoryService.retire(bookId);
        return ResponseEntity.noContent().build();
    }
}
