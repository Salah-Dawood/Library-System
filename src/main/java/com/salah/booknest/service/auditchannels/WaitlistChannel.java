package com.salah.booknest.service.auditchannels;

import com.salah.booknest.model.request.AuditTriggerInfo;
import com.salah.booknest.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WaitlistChannel implements AuditChannel {

    private final AuditLogRepository auditLogRepository;

    @Override
    public boolean supports(String type) {
        return "WAITLIST".equals(type);
    }

    @Override
    public void processAndSave(AuditTriggerInfo info) {
        auditLogRepository.save(info.toAuditLog());
    }

    @Override
    public String getReadableLog(AuditTriggerInfo info) {
        return info.action().toLowerCase() + " waitlist entry #" + info.whatId();
    }
}
