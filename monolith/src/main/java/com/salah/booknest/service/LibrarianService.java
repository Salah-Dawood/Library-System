package com.salah.booknest.service;

import com.salah.booknest.exception.InformationNotFoundException;
import com.salah.booknest.exception.InvalidRequestException;
import com.salah.booknest.exception.InvalidStateException;
import com.salah.booknest.model.User;
import com.salah.booknest.model.response.LoginResponse;
import com.salah.booknest.model.response.UserDetailResponse;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.util.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;


@Service
@RequiredArgsConstructor
public class LibrarianService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "username", "emailAddress", "role", "createdAt");

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InformationNotFoundException("User with id " + userId + " not found"));
        return UserDetailResponse.from(user);
    }

    /**
     * Returns one page of user summaries. Page size is limited to 100.
     *
     * @throws InvalidRequestException if sortBy is not an allowed field
     */
    @Transactional(readOnly = true)
    public Page<LoginResponse.UserSummary> getUsers(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageUtils.pageable(page, size, sortBy, sortDir, SORTABLE_FIELDS);
        return userRepository.findAll(pageable).map(this::toSummary);
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
