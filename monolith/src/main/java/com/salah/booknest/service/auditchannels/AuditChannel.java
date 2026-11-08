package com.salah.booknest.service.auditchannels;

import com.salah.booknest.model.request.AuditTriggerInfo;

public interface AuditChannel {
    boolean supports(String type);
    void processAndSave(AuditTriggerInfo info);
    String getReadableLog(AuditTriggerInfo info);
}
