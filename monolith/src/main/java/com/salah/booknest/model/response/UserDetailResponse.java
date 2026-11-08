package com.salah.booknest.model.response;

import com.salah.booknest.model.User;
import com.salah.booknest.model.UserProfile;

import java.time.LocalDateTime;

public record UserDetailResponse(
        Long id,
        String username,
        String emailAddress,
        String role,
        Boolean isActive,
        Boolean isVerified,
        LocalDateTime createdAt,
        Profile profile) {

    public record Profile(String firstName, String lastName, Integer age, String bio, String imageUrl) {
    }

    public static UserDetailResponse from(User user) {
        UserProfile p = user.getUserProfile();
        Profile profile = p == null ? null
                : new Profile(p.getFirstName(), p.getLastName(), p.getAge(), p.getBio(), p.getImageUrl());
        return new UserDetailResponse(user.getId(), user.getUsername(), user.getEmailAddress(), user.getRole(),
                user.getIsActive(), user.getIsVerified(), user.getCreatedAt(), profile);
    }
}
