package com.salah.booknest.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsernameRequest {

    @NotEmpty(message = "Username can not be empty")
    @Size(min = 3,max = 15,message = "Username must be between 3 to 15 characters")
    private String username;
}
