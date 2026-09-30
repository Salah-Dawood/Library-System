package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.LoginRequest;
import com.salah.booknest.model.request.RegisterRequest;
import com.salah.booknest.model.response.LoginResponse;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.security.JWTUtils;
import com.salah.booknest.security.JwtRequestFilter;
import com.salah.booknest.security.MyUserDetails;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponse;

import java.util.Random;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private MyUserDetails myUserDetails;
    private EmailVerificationService emailVerificationService;

    public UserService(UserRepository userRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       JWTUtils jwtUtils,
                       @Lazy AuthenticationManager authenticationManager,
                       @Lazy MyUserDetails myUserDetails,
                       EmailVerificationService emailVerificationService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.myUserDetails = myUserDetails;
        this.emailVerificationService = emailVerificationService;
    }

    //Find user services

    public User findUserById(Long userId){
        System.out.println("Finding user by ID");
        return userRepository.findUserById(userId);
    }

    public User findUserByUsername(String username){
        System.out.println("Finding user by username " + username);
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
    }



    public User createUser(RegisterRequest request){
        System.out.println("service calling createUser()");
        if(userRepository.existsByUsername(request.getUsername())){
            throw new InformationExistException("User with username " + request.getUsername() + " already exists");
        } else if (userRepository.existsByEmailAddress(request.getEmailAddress())){
            throw new InformationExistException("User with Email address " + request.getEmailAddress() + " already exists");
        } else {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmailAddress(request.getEmailAddress());
            user.setUserProfile(request.getUserProfile());
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            return userRepository.save(user);
        }
    }

    public ResponseEntity<?> loginUser(LoginRequest loginRequest) {
        System.out.println("Processing login for: " + loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        MyUserDetails principalDetails = (MyUserDetails) authentication.getPrincipal();
        User dbUser = principalDetails.getUser();

//        verify user is verified
        if (!dbUser.getIsVerified()) {
            java.util.Map<String, String> errorDetails = new java.util.HashMap<>();
            errorDetails.put("status", "USER_NOT_VERIFIED");
            errorDetails.put("message", "Please verify your email before logging in.");

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDetails);
        }

        //generate jwt
        final String jwt = jwtUtils.generateJwtToken(principalDetails);

        // create user summary for front end
        LoginResponse.UserSummary summary = new LoginResponse.UserSummary(
                dbUser.getId(),
                dbUser.getUsername(),
                dbUser.getEmailAddress(),
                dbUser.getRole(),
                dbUser.getIsActive()
        );

        return ResponseEntity.ok(new LoginResponse(jwt, summary));
    }

    public ResponseEntity<String> executePasswordReset(String token,String password){
        if (!jwtUtils.validateJwtToken(token)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to reset password: Link has expired or is invalid.");
        }

        // 2. Extract the username embedded inside the token payload
        String username = jwtUtils.getUserNameFromJwtToken(token);

        // 3. Find the user in the database
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));

        // 4. Encrypt the password parameter and save it
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        // 5. Return a user-friendly browser response string
        return ResponseEntity.ok("Success! Your password has been updated to: " + password +
                ". You can close this tab and log in now.");
    }
}
