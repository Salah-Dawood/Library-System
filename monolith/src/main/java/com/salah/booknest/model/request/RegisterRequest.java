package com.salah.booknest.model.request;

import com.salah.booknest.model.UserProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class RegisterRequest {

    @NotEmpty(message = "Username can not be empty!!")
    @Size(min = 3,max = 15, message = "Username length must between 3 and 15 characters")
    private String username;

    @Email(message = "Please enter a valid email")
    private String emailAddress;

    @NotEmpty(message = "Password can not be empty")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotEmpty(message = "First name can not be empty")
    private String firstName;

    @NotEmpty(message = "Last name can not be empty")
    private String lastName;

    @Size(max = 1000)
    private String bio;

    @NotNull(message = "Age can not be null")
    @Range(min = 16, message = "Users under 16 are not allowed to create account")
    @Range(min = 0,max = 100,message = "Please enter a realistic age")
    private Integer age;
}