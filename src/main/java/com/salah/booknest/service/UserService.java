package com.salah.booknest.service;

import com.salah.booknest.exception.InformationExistException;
import com.salah.booknest.model.User;
import com.salah.booknest.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;


    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User createUser(User userObject){
        System.out.println("calling createUser()");
        if(userRepository.existsByUsername(userObject.getUsername())){
            throw new InformationExistException("User with username " + userObject.getUsername() + " already exists");
        } else if (userRepository.existsByEmailAddress(userObject.getEmailAddress())){
            throw new InformationExistException("User with Email address " + userObject.getEmailAddress() + " already exists");
        } else {
            return userRepository.save(userObject);
        }
    }
}
