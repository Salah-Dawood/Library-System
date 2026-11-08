package com.salah.booknest.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private final String type = "Bearer";
    private UserSummary user;

    @Data
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String username;
        private String emailAddress;
        private String role;
        private Boolean isActive;
    }
}
