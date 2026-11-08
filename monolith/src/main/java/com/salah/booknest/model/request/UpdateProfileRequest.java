package com.salah.booknest.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateProfileRequest {

    @NotEmpty(message = "First name can not be empty")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotEmpty(message = "Last name can not be empty")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Size(max = 250, message = "Bio cannot exceed 250 characters")
    private String bio;

    @NotNull(message = "Age can not be null")
    @Min(value = 16, message = "You must be at least 16 years old")
    private Integer age;

    private MultipartFile image;
}
