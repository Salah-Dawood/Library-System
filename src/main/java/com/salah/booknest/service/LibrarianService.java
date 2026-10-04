package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.response.LoginResponse;
import com.salah.booknest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class LibrarianService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public List<LoginResponse.UserSummary> getUsers() {
        return userRepository.findAll().stream().map(this::toSummary).toList();
    }

    @Transactional
    public LoginResponse.UserSummary deactivateUser(Long userId, String librarian) {
        User user = getOtherUser(userId, librarian);
        user.setIsActive(false);
        User saved = userRepository.save(user);
        auditLogService.log("USER", "DEACTIVATED", userId);
        return toSummary(saved);
    }

    @Transactional
    public LoginResponse.UserSummary activateUser(Long userId, String librarian) {
        User user = getOtherUser(userId, librarian);
        user.setIsActive(true);
        User saved = userRepository.save(user);
        auditLogService.log("USER", "ACTIVATED", userId);
        return toSummary(saved);
    }

    @Transactional
    public void deleteUser(Long userId, String librarian) {
        User user = getOtherUser(userId, librarian);
        // Audited first so the entry can still name the user it is about.
        auditLogService.log("USER", "DELETED", userId);
        userRepository.delete(user);
    }

    private User getOtherUser(Long userId, String librarian) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + userId + " not found"));
        if (user.getUsername().equals(librarian)) {
            throw new InvalidStateException("You cannot change or delete your own account here");
        }
        return user;
    }

    private LoginResponse.UserSummary toSummary(User user) {
        return new LoginResponse.UserSummary(user.getId(), user.getUsername(),
                user.getEmailAddress(), user.getRole(), user.getIsActive());
    }
}
