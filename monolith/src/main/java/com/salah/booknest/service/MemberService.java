package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.UserProfile;
import com.salah.booknest.model.request.UpdateProfileRequest;
import com.salah.booknest.model.response.MessageResponse;
import com.salah.booknest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Service
public class MemberService {

    private final String PROFILE_DIR = "profile/";
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif");
    private static final int MAX_IMAGE_SIDE_PX = 5000;


    @Autowired
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public MemberService(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public MessageResponse changePassword(String username, String currentPassword, String newPassword){
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("Username " + username + " not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.logAs("USER", user.getId(), "PASSWORD_CHANGED", user.getId());
        return new MessageResponse("Password changed successfully");
    }

    public MessageResponse updateProfile(String username, UpdateProfileRequest request){
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
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            profile.setImageUrl(uploadImage(request.getImage()));
        }

        userRepository.save(user);
        auditLogService.logAs("USER", user.getId(), "PROFILE_UPDATED", user.getId());
        return new MessageResponse("Profile updated successfully");
    }

    //used to upload image and return URL
    public String uploadImage(MultipartFile image){
        if (image.isEmpty()) {
            throw new InvalidRequestException("Image file is empty");
        }

        // 1. The declared type must be an allowed image type
        String declaredType = image.getContentType();
        if (declaredType == null || !ALLOWED_IMAGE_TYPES.contains(declaredType.toLowerCase())) {
            throw new InvalidRequestException("Only JPEG, PNG and GIF images are allowed");
        }

        // 2. The real content must be one of those image types (the declared type can be faked)
        String extension = detectImageExtension(image);
        if (extension == null) {
            throw new InvalidRequestException("The file is not a valid JPEG, PNG or GIF image");
        }

        try {
            Path uploadPath = Paths.get(PROFILE_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // The client's filename is never used: the name is generated and the extension
            // comes from the detected content type
            String fileName = UUID.randomUUID() + "." + extension;

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

    /**
     * Looks at the file's content (not its name or declared type) and returns "jpg", "png" or "gif",
     * or null if it is not a valid image of one of those types.
     */
    private String detectImageExtension(MultipartFile image) {
        try (ImageInputStream in = ImageIO.createImageInputStream(image.getInputStream())) {
            if (in == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                // Reading the size also checks that the image header is valid
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_IMAGE_SIDE_PX || height > MAX_IMAGE_SIDE_PX) {
                    return null;
                }
                return switch (reader.getFormatName().toLowerCase()) {
                    case "jpeg" -> "jpg";
                    case "png" -> "png";
                    case "gif" -> "gif";
                    default -> null;
                };
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public UserProfile getProfile(String username) {
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + username + " not found"));
        return user.getUserProfile();
    }
}
