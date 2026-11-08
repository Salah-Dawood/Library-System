package com.salah.booknest.lending.service.auditchannels;

import com.salah.booknest.lending.model.request.AuditTriggerInfo;
import com.salah.booknest.lending.repository.AuditLogRepository;
import com.salah.booknest.lending.repository.LoanRepository;
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
                .map(l -> "loan #" + l.getId() + " of '" + l.getBookTitle() + "' for " + l.getUsername())
                .orElse("loan #" + info.whatId());
        return info.action().toLowerCase() + " " + target;
    }
}
