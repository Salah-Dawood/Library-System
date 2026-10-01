package com.salah.booknest.controller;

import com.salah.booknest.model.User;
import com.salah.booknest.model.UserProfile;
import com.salah.booknest.model.request.ChangePasswordRequest;
import com.salah.booknest.model.request.UpdateProfileRequest;
import com.salah.booknest.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                                 @RequestBody ChangePasswordRequest request){
        String result = memberService.changePassword(userDetails.getUsername(), request.getNewPassword());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update/profile")
    public ResponseEntity<String> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                 @ModelAttribute UpdateProfileRequest request){
        String result = memberService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/profile")
    public UserProfile getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return memberService.getProfile(userDetails.getUsername());
    }
}
