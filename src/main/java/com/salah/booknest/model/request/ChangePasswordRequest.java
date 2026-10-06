package com.salah.booknest.model.request;


import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
public class ChangePasswordRequest {

    @Min(value = 8, message = "password must be at least 8 characters")
    private String newPassword;
}
