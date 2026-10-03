package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.response.LoginResponse;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.security.JWTUtils;
import com.salah.booknest.security.MyUserDetails;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LibrarianService {

    private final UserRepository userRepository;

    public LibrarianService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public List<LoginResponse.UserSummary> getUsers() {
        return userRepository.findAll().stream()
                .map(u -> new LoginResponse.UserSummary(u.getId(), u.getUsername(),
                        u.getEmailAddress(), u.getRole(), u.getIsActive())).toList();
    }

    public User deactivateUser(Long userId){
        System.out.println("Deactivating user: " + userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + userId + " not found"));

        user.setIsActive(false);
        return userRepository.save(user);
    }

    public User activateUser(Long userId){
        System.out.println("Activating user: " + userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + userId + " not found"));

        user.setIsActive(true);
        return userRepository.save(user);
    }

    public User deleteUser(Long userId){
        System.out.println("Service deleting user: " + userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + userId + " not found"));
        userRepository.deleteById(userId);
        return userRepository.findUserById(userId);
    }
}
