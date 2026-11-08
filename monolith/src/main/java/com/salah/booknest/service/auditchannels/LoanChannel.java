package com.salah.booknest.service.auditchannels;

import com.salah.booknest.model.request.AuditTriggerInfo;
import com.salah.booknest.repository.AuditLogRepository;
import com.salah.booknest.repository.LoanRepository;
import org.springframework.stereotype.Component;

@Component
public class LoanChannel implements AuditChannel {

    private final AuditLogRepository auditLogRepository;
    private final LoanRepository loanRepository;

    public LoanChannel(AuditLogRepository auditLogRepository, LoanRepository loanRepository) {
        this.auditLogRepository = auditLogRepository;
        this.loanRepository = loanRepository;
    }

    @Override
    public boolean supports(String type) {
        return "LOAN".equals(type);
    }

    @Override
    public void processAndSave(AuditTriggerInfo info) {
        auditLogRepository.save(info.toAuditLog());
    }

    @Override
    public String getReadableLog(AuditTriggerInfo info) {
        String target = loanRepository.findById(info.whatId())
                .map(l -> "loan #" + l.getId() + " of '" + l.getBook().getTitle() + "' for " + l.getUser().getUsername())
                .orElse("loan #" + info.whatId());
        return info.action().toLowerCase() + " " + target;
    }
}
