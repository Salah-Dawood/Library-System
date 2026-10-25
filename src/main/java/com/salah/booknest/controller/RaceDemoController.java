package com.salah.booknest.controller;

import com.salah.booknest.demo.RaceDemoService;
import com.salah.booknest.demo.RaceResult;
import com.salah.booknest.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Race demo", description = "Only exists with the race-demo profile. Shows what happens when many threads take the last copy of a book.")
@RestController
@RequestMapping("/api/demo/race")
@Profile("race-demo")
@RequiredArgsConstructor
public class RaceDemoController {

    private final RaceDemoService raceDemoService;

    @Operation(summary = "Run the last copy race", description = "Use a book that has no active loans. Its stock is set to 1 copy for the test and restored after. safe=false shows the bug, safe=true shows the fix.")
    @PostMapping("/{bookId}")
    @PreAuthorize("hasRole('librarian')")
    public RaceResult run(@PathVariable Long bookId,
                          @RequestParam(defaultValue = "10") int threads,
                          @RequestParam(defaultValue = "false") boolean safe) throws InterruptedException {
        if (threads < 2 || threads > 20) {
            throw new InvalidRequestException("Threads must be between 2 and 20");
        }
        return raceDemoService.run(bookId, threads, safe);
    }
}
