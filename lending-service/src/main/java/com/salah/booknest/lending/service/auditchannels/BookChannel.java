package com.salah.booknest.lending.service.auditchannels;

import com.salah.booknest.lending.model.request.AuditTriggerInfo;
import com.salah.booknest.lending.repository.AuditLogRepository;
import com.salah.booknest.lending.repository.BookRepository;
import org.springframework.stereotype.Component;

@Component
public class BookChannel implements AuditChannel {

    private final AuditLogRepository auditLogRepository;
    private final BookRepository bookRepository;

    public BookChannel(AuditLogRepository auditLogRepository, BookRepository bookRepository) {
        this.auditLogRepository = auditLogRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    public boolean supports(String type) {
        return "BOOK".equals(type);
    }

    @Override
    public void processAndSave(AuditTriggerInfo info) {
        auditLogRepository.save(info.toAuditLog());
    }

    @Override
    public String getReadableLog(AuditTriggerInfo info) {
        String target = bookRepository.findById(info.whatId())
                .map(b -> "book '" + b.getTitle() + "' (id " + b.getId() + ")")
                .orElse("book #" + info.whatId());
        return info.action().toLowerCase() + " " + target;
    }
}
