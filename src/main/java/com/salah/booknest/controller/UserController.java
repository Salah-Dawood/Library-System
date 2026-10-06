package com.salah.booknest.controller;

import com.salah.booknest.model.response.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.ChangePasswordRequest;
import com.salah.booknest.model.request.LoginRequest;
import com.salah.booknest.model.request.RegisterRequest;
import com.salah.booknest.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Register, log in and reset a password. Public: no token needed.")
@RestController
@AllArgsConstructor
@RequestMapping("/auth/users")
public class UserController {
    private UserService userService;

    @Operation(summary = "Register", description = "Creates an unverified account. Then request a code with PUT /auth/email/getverification/{username} and verify it. Public: no token needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created", content = @Content(schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already registered"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"username\":\"john\",\"emailAddress\":\"john@example.com\",\"password\":\"Passw0rd!\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"age\":25,\"bio\":\"Loves science fiction\"}")))
    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {

        User createdUser = userService.createUser(request);

        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @Operation(summary = "Log in", description = "Returns a JWT. Send it as Authorization: Bearer token. Unverified accounts get 403 with status USER_NOT_VERIFIED. Public: no token needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Wrong username or password"),
            @ApiResponse(responseCode = "403", description = "Email not verified (USER_NOT_VERIFIED) or account deactivated (ACCOUNT_DEACTIVATED)"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"username\":\"john\",\"password\":\"Passw0rd!\"}")))
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest){
        return userService.loginUser(loginRequest);
    }

    @Operation(summary = "Set a new password with a reset token", description = "The token comes from the email link and only works for password reset, not as a login token. Public: no token needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "The reset link is invalid or has expired"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"newPassword\":\"NewPassw0rd!\"}")))
    @PutMapping("/passwordreset/{token}")
    public ResponseEntity<String> executePasswordReset(
            @Parameter(description = "Password-reset token from the email link") @PathVariable("token") String token,
            @Valid@RequestBody ChangePasswordRequest changePasswordRequest) {
        return userService.executePasswordReset(token, changePasswordRequest.getNewPassword());
    }
}
