package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.model.UserProfile;
import com.salah.booknest.model.request.ChangePasswordRequest;
import com.salah.booknest.model.request.UpdateProfileRequest;
import com.salah.booknest.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member account", description = "Password and profile of the logged-in user.")
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @Operation(summary = "Change my password", description = "Changes the password of the logged-in user.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "{\"newPassword\":\"NewPassw0rd!\"}")))
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                                 @RequestBody ChangePasswordRequest request){
        String result = memberService.changePassword(userDetails.getUsername(), request.getNewPassword());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Update my profile", description = "Multipart form with any of: firstName, lastName, bio, age, image (a file). Only the fields you send are changed.")
    @ApiResponses({
            @ApiResponse(responseCode = "413", description = "Image is too large")
    })
    @PutMapping("/update/profile")
    public ResponseEntity<String> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                 @Valid @ModelAttribute UpdateProfileRequest request){
        String result = memberService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get my profile", description = "Profile of the logged-in user, including the picture path (imageUrl).")
    @GetMapping("/profile")
    public UserProfile getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return memberService.getProfile(userDetails.getUsername());
    }
}
