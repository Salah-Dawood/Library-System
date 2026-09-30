package com.salah.booknest.model.request;

import com.salah.booknest.model.UserProfile;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String emailAddress;
    private String password;
    private UserProfile userProfile;
}