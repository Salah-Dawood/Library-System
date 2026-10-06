package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.UsernameRequest;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Email verification and password recovery", description = "Public endpoints for the email verification code and the password-reset email.")
@RestController
@RequestMapping("/auth/email")
public class EmailVerificationController {

    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    private UserRepository userRepository;



    @Operation(summary = "Send the email verification code", description = "Emails a new code to the user. Returns the text SENT. Public: no token needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Username not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PutMapping("/getverification/{username}")
    public String sendVerificationCode(@Parameter(description = "Username") @PathVariable String username){
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
        emailVerificationService.sendVerificationCode(user);
        return "SENT";
    }

    @Operation(summary = "Verify an email address", description = "Marks the account as verified when the code matches. Unverified users cannot log in. Public: no token needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Incorrect verification code"),
            @ApiResponse(responseCode = "404", description = "Username not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PutMapping("/verify/{username}")
    public String vertifyEmail(@Parameter(description = "Username") @PathVariable String username, @Parameter(description = "Verification code from the email") @RequestParam int code){
        return emailVerificationService.verifyEmail(username,code);
    }

    @Operation(summary = "Send a password-reset email", description = "Emails a link to the password-reset page, valid for a short time. Public: no token needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Username not found"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"username\":\"john\"}")))
    @PutMapping("/forgotpassword")
    public String sendResetEmail(@Valid @RequestBody UsernameRequest request){
        emailVerificationService.sendResetEmail(request);
        return "reset should be sent by now";
    }
}
