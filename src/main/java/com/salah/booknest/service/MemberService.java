package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.UserProfile;
import com.salah.booknest.model.request.UpdateProfileRequest;
import com.salah.booknest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class MemberService {

    private final String PROFILE_DIR = "profile/";


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

    public String updateProfile(String username, UpdateProfileRequest request){
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("User not found"));

        UserProfile profile = user.getUserProfile();
        if (profile == null) {
            profile = new UserProfile();
            user.setUserProfile(profile);
        }

        if (request.getFirstName() != null) {
            profile.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            profile.setLastName(request.getLastName());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getAge() != null) {
            profile.setAge(request.getAge());
        }
        if (request.getImage() != null) {
            profile.setImageUrl(uploadImage(request.getImage()));
        }

        userRepository.save(user);
        return "profile should be updated";
    }

    //used to upload image and return URL
    public String uploadImage(MultipartFile image){
        try {
            Path uploadPath = Paths.get(PROFILE_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = image.getOriginalFilename();

            //generate unique id
            String uniqueId = UUID.randomUUID().toString();

            String fileName = uniqueId + "_" + originalFileName;

            // Create file path
            Path filePath = uploadPath.resolve(fileName);

            // Save image
            image.transferTo(filePath);

            // Save image path in database
            return filePath.toString();

        } catch (IOException e) {
            throw new RuntimeException("Could not save image", e);
        }
    }

    public UserProfile getProfile(String username) {
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + username + " not found"));
        return user.getUserProfile();
    }
}
