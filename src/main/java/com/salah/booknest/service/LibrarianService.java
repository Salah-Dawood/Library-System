package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.response.LoginResponse;
import com.salah.booknest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/** Librarian-only user administration. Every action is logged with the librarian who performed it. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LibrarianService {

    private final UserRepository userRepository;

    public List<LoginResponse.UserSummary> getUsers() {
        return userRepository.findAll().stream().map(this::toSummary).toList();
    }

    public LoginResponse.UserSummary deactivateUser(Long userId, String librarian) {
        User user = getOtherUser(userId, librarian);
        user.setIsActive(false);
        log.info("User {} deactivated by {}", user.getUsername(), librarian);
        return toSummary(userRepository.save(user));
    }

    public LoginResponse.UserSummary activateUser(Long userId, String librarian) {
        User user = getOtherUser(userId, librarian);
        user.setIsActive(true);
        log.info("User {} activated by {}", user.getUsername(), librarian);
        return toSummary(userRepository.save(user));
    }

    public void deleteUser(Long userId, String librarian) {
        User user = getOtherUser(userId, librarian);
        userRepository.delete(user);
        log.info("User {} deleted by {}", user.getUsername(), librarian);
    }

    /** Loads the user and refuses actions that would lock a librarian out of their own account. */
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
