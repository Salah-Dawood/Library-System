package com.salah.booknest.service.auditchannels;

import com.salah.booknest.model.request.AuditTriggerInfo;
import com.salah.booknest.repository.AuditLogRepository;
import com.salah.booknest.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserChannel implements AuditChannel {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public UserChannel(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean supports(String type) {
        return "USER".equals(type);
    }

    @Override
    public void processAndSave(AuditTriggerInfo info) {
        auditLogRepository.save(info.toAuditLog());
    }

    @Override
    public String getReadableLog(AuditTriggerInfo info) {
        String target = userRepository.findById(info.whatId())
                .map(user -> "user '" + user.getUsername() + "' (id " + user.getId() + ")")
                .orElse("user #" + info.whatId());
        return switch (info.action()) {
            // These are always done by users on their own account.
            case "REGISTERED" -> "registered a new account";
            case "PASSWORD_CHANGED" -> "changed their password";
            case "PROFILE_UPDATED" -> "updated their profile";
            default -> info.action().toLowerCase() + " " + target;
        };
    }
}
