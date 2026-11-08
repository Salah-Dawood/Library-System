package com.salah.booknest.service.auditchannels;

import com.salah.booknest.model.request.AuditTriggerInfo;
import com.salah.booknest.repository.AuditLogRepository;
import com.salah.booknest.repository.FineRepository;
import org.springframework.stereotype.Component;

@Component
public class FineChannel implements AuditChannel {

    private final AuditLogRepository auditLogRepository;
    private final FineRepository fineRepository;

    public FineChannel(AuditLogRepository auditLogRepository, FineRepository fineRepository) {
        this.auditLogRepository = auditLogRepository;
        this.fineRepository = fineRepository;
    }

    @Override
    public boolean supports(String type) {
        return "FINE".equals(type);
    }

    @Override
    public void processAndSave(AuditTriggerInfo info) {
        auditLogRepository.save(info.toAuditLog());
    }

    @Override
    public String getReadableLog(AuditTriggerInfo info) {
        String target = fineRepository.findById(info.whatId())
                .map(f -> f.getType().name().toLowerCase() + " fine #" + f.getId() + " of " + f.getAmount()
                        + " for loan #" + f.getLoan().getId() + " ('" + f.getLoan().getBook().getTitle()
                        + "' for " + f.getLoan().getUser().getUsername() + ")")
                .orElse("fine #" + info.whatId());
        return info.action().toLowerCase() + " " + target;
    }
}
