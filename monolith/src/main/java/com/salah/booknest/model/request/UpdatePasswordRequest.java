package com.salah.booknest.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UpdatePasswordRequest {

    @NotEmpty(message = "current password can not be empty")
    private String currentPassword;

    @NotEmpty(message = "password must be at least 8 characters")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String newPassword;
}
