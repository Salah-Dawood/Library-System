package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.UserProfile;
import com.salah.booknest.model.request.LoginRequest;
import com.salah.booknest.model.request.RegisterRequest;
import com.salah.booknest.model.response.LoginResponse;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.security.JWTUtils;
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


@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private MyUserDetails myUserDetails;
    private EmailVerificationService emailVerificationService;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       JWTUtils jwtUtils,
                       @Lazy AuthenticationManager authenticationManager,
                       @Lazy MyUserDetails myUserDetails,
                       EmailVerificationService emailVerificationService,
                       AuditLogService auditLogService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.myUserDetails = myUserDetails;
        this.emailVerificationService = emailVerificationService;
        this.auditLogService = auditLogService;
    }

    //Find user services

    public User findUserById(Long userId){
        return userRepository.findUserById(userId);
    }

    public User findUserByUsername(String username){
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
    }



    public User createUser(RegisterRequest request){
        if(userRepository.existsByUsername(request.getUsername())){
            throw new InformationExistException("User with username " + request.getUsername() + " already exists");
        } else if (userRepository.existsByEmailAddress(request.getEmailAddress())){
            throw new InformationExistException("User with Email address " + request.getEmailAddress() + " already exists");
        } else {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmailAddress(request.getEmailAddress());
            user.setPassword(passwordEncoder.encode(request.getPassword()));

            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setFirstName(request.getFirstName());
            profile.setLastName(request.getLastName());
            profile.setAge(request.getAge());
            profile.setBio(request.getBio());

            user.setUserProfile(profile);
            User saved = userRepository.save(user);
            auditLogService.logAs("USER", saved.getId(), "REGISTERED", saved.getId());
            return saved;
        }
    }

    /**
     * Authenticates the user and returns a JWT with a user summary. Users who have not verified
     * their email get a 403 response with status USER_NOT_VERIFIED instead of a token.
     */
    public ResponseEntity<?> loginUser(LoginRequest loginRequest) {

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

    /**
     * Sets a new password using the token from the reset email.
     * Returns 400 if the token is expired, invalid, or not a password-reset token.
     */
    public ResponseEntity<String> executePasswordReset(String token,String password){
        if (!jwtUtils.validateJwtToken(token) || !jwtUtils.isPasswordResetToken(token)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to reset password: Link has expired or is invalid.");
        }

        String username = jwtUtils.getUserNameFromJwtToken(token);

        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        return ResponseEntity.ok("Success! Your password has been updated\n" +
                "You can close this tab and log in now.");
    }
}
