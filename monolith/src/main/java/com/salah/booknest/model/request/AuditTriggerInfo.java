package com.salah.booknest.model.request;

import com.salah.booknest.model.AuditLog;

import java.time.LocalDateTime;

public record AuditTriggerInfo(
        String type,
        Long userId,
        Long whatId,
        String action,
        LocalDateTime timestamp
) {

    public AuditLog toAuditLog() {
        AuditLog entry = new AuditLog();
        entry.setType(type);
        entry.setUserId(userId);
        entry.setAction(action);
        entry.setWhatId(whatId);
        return entry;
    }
}
