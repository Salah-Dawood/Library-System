package com.salah.booknest.lending.service.auditchannels;

import com.salah.booknest.lending.model.request.AuditTriggerInfo;

public interface AuditChannel {
    boolean supports(String type);
    void processAndSave(AuditTriggerInfo info);
    String getReadableLog(AuditTriggerInfo info);
}
