package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.User;
import com.salah.booknest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    @Autowired
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(UserRepository userRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String changePassword(String username,String password){
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return "password changed";
    }
}
