package com.salah.booknest.demo;

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

    @Operation(summary = "Run the loan limit race", description = "One member (use a seeded member) tries to borrow several different books at the same moment. Needs as many books with a free copy as threads. safe=false skips the member lock, so the member can end up with more than 5 loans. safe=true uses the real borrow. The temporary loans are removed afterwards, audit entries from the safe run stay.")
    @PostMapping("/loan-limit/{username}")
    @PreAuthorize("hasRole('librarian')")
    public LoanLimitRaceResult runLoanLimit(@PathVariable String username,
                                            @RequestParam(defaultValue = "8") int threads,
                                            @RequestParam(defaultValue = "false") boolean safe) throws InterruptedException {
        if (threads < 6 || threads > 20) {
            throw new InvalidRequestException("Threads must be between 6 and 20");
        }
        return raceDemoService.runLoanLimit(username, threads, safe);
    }
}
