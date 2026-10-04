package com.salah.booknest.model.request;

import java.time.LocalDateTime;

public record AuditTriggerInfo(
        String type,
        Long userId,
        Long whatId,
        String action,
        LocalDateTime timestamp
) {}
