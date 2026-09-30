package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.request.LoginRequest;
import com.salah.booknest.model.request.RegisterRequest;
import com.salah.booknest.model.response.LoginResponse;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.security.JWTUtils;
import com.salah.booknest.security.JwtRequestFilter;
import com.salah.booknest.security.MyUserDetails;
import org.springframework.context.annotation.Lazy;
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

    public UserService(UserRepository userRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       JWTUtils jwtUtils,
                       @Lazy AuthenticationManager authenticationManager,
                       @Lazy MyUserDetails myUserDetails){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.myUserDetails = myUserDetails;
    }

    //Find user services

    public User findUserById(Long userId){
        System.out.println("Finding user by ID");
        return userRepository.findUserById(userId);
    }

    public User findUserByUsername(String username){
        System.out.println("Finding user by username " + username);
        return userRepository.findUserByUsername(username);
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


    public ResponseEntity<LoginResponse> loginUser(LoginRequest loginRequest) {
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
}
