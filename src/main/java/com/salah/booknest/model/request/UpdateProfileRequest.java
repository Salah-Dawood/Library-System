package com.salah.booknest.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Size(max = 250, message = "Bio cannot exceed 250 characters")
    private String bio;

    @Min(value = 16, message = "You must be at least 16 years old")
    private Integer age;

    private MultipartFile image;
}
