package com.salah.booknest.model.request;

import com.salah.booknest.model.UserProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotEmpty(message = "Username can not be empty!!")
    private String username;

    @Email(message = "Please enter a valid email")
    private String emailAddress;

    @NotEmpty(message = "Password can not be empty")
    private String password;

    @NotNull(message = "User profile can not be null")
    @Valid
    private UserProfile userProfile;
}