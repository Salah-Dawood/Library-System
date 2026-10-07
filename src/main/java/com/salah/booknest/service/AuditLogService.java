package com.salah.booknest.service;

import com.salah.booknest.model.User;
import com.salah.booknest.model.request.AuditTriggerInfo;
import com.salah.booknest.model.response.AuditLogResponse;
import com.salah.booknest.repository.AuditLogRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.service.auditchannels.AuditChannel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT LOG");

    private final List<AuditChannel> channels;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Records an audit entry for the user who is currently logged in.
     * If nobody is authenticated the entry is recorded as done by the system.
     */
    @Transactional
    public void log(String type, String action, Long whatId) {
        routeLog(new AuditTriggerInfo(type, currentUserId(), whatId, action, LocalDateTime.now()));
    }

    /**
     * Records an audit entry for an explicit user id, for actions where no one is logged in yet (for example registration).
     */
    @Transactional
    public void logAs(String type, Long userId, String action, Long whatId) {
        routeLog(new AuditTriggerInfo(type, userId, whatId, action, LocalDateTime.now()));
    }

    /**
     * Hands the entry to the first audit channel that supports its type, which saves it,
     * and writes a readable line to the audit logger once the transaction commits.
     *
     * @throws IllegalArgumentException if no channel supports the entry type
     */
    @Transactional
    public void routeLog(AuditTriggerInfo info) {
        AuditChannel matchingChannel = channels.stream()
                .filter(channel -> channel.supports(info.type()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No audit channel found for type " + info.type()));

        matchingChannel.processAndSave(info);
        String readableLog = describeActor(info.userId()) + " " + matchingChannel.getReadableLog(info);
        afterCommit(() -> AUDIT.info(readableLog));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(String type, Long userId) {
        return auditLogRepository.search(type == null ? null : type.toUpperCase(), userId).stream()
                .map(AuditLogResponse::from).toList();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findUserByUsername(authentication.getName()).map(User::getId).orElse(null);
    }

    private String describeActor(Long userId) {
        if (userId == null) {
            return "System";
        }
        return userRepository.findById(userId)
                .map(user -> user.getUsername() + " (id " + userId + ")")
                .orElse("user #" + userId);
    }

    /**
     * Defers the action until the transaction commits; runs it immediately when there is no active transaction.
     */
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
