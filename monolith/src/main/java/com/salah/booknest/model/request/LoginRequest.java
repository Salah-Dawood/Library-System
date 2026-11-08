package com.salah.booknest.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

@Getter
public class LoginRequest {

    @NotEmpty(message = "Username can not be empty!")
    private String username;

    @NotEmpty(message = "password can not be empty")
    private String password;
}
