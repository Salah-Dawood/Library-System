package com.salah.booknest.service.auditchannels;

import com.salah.booknest.model.request.AuditTriggerInfo;
import com.salah.booknest.repository.AuditLogRepository;
import com.salah.booknest.repository.ReviewRepository;
import org.springframework.stereotype.Component;

@Component
public class ReviewChannel implements AuditChannel {

    private final AuditLogRepository auditLogRepository;
    private final ReviewRepository reviewRepository;

    public ReviewChannel(AuditLogRepository auditLogRepository, ReviewRepository reviewRepository) {
        this.auditLogRepository = auditLogRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public boolean supports(String type) {
        return "REVIEW".equals(type);
    }

    @Override
    public void processAndSave(AuditTriggerInfo info) {
        auditLogRepository.save(info.toAuditLog());
    }

    @Override
    public String getReadableLog(AuditTriggerInfo info) {
        String target = reviewRepository.findById(info.whatId())
                .map(r -> "review #" + r.getId() + " (rating " + r.getRating() + ") on '" + r.getBook().getTitle() + "' by " + r.getUser().getUsername())
                .orElse("review #" + info.whatId());
        return info.action().toLowerCase() + " " + target;
    }
}
