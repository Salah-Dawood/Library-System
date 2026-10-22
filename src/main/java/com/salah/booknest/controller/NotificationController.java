package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.security.Roles;
import com.salah.booknest.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notifications", description = "Live events over Server-Sent Events.")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Live notifications", description = "Opens a Server-Sent Events stream. Members get events about their own loans; librarians also get new-request and cancellation events. Send the Authorization header (use fetch or Postman, not the browser EventSource).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A stream that stays open. Events: CONNECTED, LOAN_RETURNED (librarians: a returned book needs review), RETURN_REVIEWED (member: a librarian reviewed their return), LOAN_CANCELLED.",
            content = @Content(mediaType = "text/event-stream",
                    examples = @ExampleObject(value = "event:RETURN_REVIEWED\ndata:{\"type\":\"RETURN_REVIEWED\",\"message\":\"Your return of Dune was reviewed and accepted. Thank you!\",\"loanId\":7,\"status\":\"RETURNED\"}")))
    })
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        return notificationService.subscribe(authentication.getName(), Roles.isLibrarian(authentication));
    }
}