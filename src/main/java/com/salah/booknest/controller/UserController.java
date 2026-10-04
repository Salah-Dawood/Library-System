package com.salah.booknest.controller;

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

@RestController
@AllArgsConstructor
@RequestMapping("/auth/users")
public class UserController {
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {

        User createdUser = userService.createUser(request);

        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest){
        System.out.println("calling loginUser()");
        return userService.loginUser(loginRequest);
    }

    @PutMapping("/passwordreset/{token}")
    public ResponseEntity<String> executePasswordReset(
            @PathVariable("token") String token,
            @RequestBody ChangePasswordRequest changePasswordRequest) {
        return userService.executePasswordReset(token, changePasswordRequest.getNewPassword());
    }




}
