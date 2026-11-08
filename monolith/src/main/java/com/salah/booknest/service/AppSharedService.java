package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.User;
import com.salah.booknest.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AppSharedService {

    private final UserRepository userRepository;

    public AppSharedService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserFromAuth(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
    }

    public User getUserFromAuthForUpdate(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findWithLockByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));
    }
}
