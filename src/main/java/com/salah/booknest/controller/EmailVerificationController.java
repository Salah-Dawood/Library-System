package com.salah.booknest.controller;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.UsernameRequest;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.service.EmailVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Past;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/email")
public class EmailVerificationController {

    @Autowired
    private EmailVerificationService emailVerificationService;
    @Autowired
    private UserRepository userRepository;



    @PutMapping("/getverification/{username}")
    public String sendVerificationCode(@PathVariable String username){
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
        System.out.println("calling sendVerificationCode() to " + user.getEmailAddress());
        emailVerificationService.sendVerificationCode(user);
        return "SENT";
    }

    @PutMapping("/verify/{username}")
    public String vertifyEmail(@PathVariable String username, @RequestParam int code){
        System.out.println();
        return emailVerificationService.verifyEmail(username,code);
    }

    @PutMapping("/forgotpassword")
    public String sendResetEmail(@Valid @RequestBody UsernameRequest request){
        emailVerificationService.sendResetEmail(request);
        return "reset should be sent by now";
    }
}
