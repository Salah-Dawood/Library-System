package com.salah.booknest.model.response;

import com.salah.booknest.model.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String type;
    private Long userId;
    private String action;
    private Long whatId;
    private LocalDateTime timestamp;

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getType(), log.getUserId(),
                log.getAction(), log.getWhatId(), log.getTimestamp());
    }
}
