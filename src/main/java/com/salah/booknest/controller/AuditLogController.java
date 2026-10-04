package com.salah.booknest.controller;

import com.salah.booknest.model.AuditLog;
import com.salah.booknest.model.response.AuditLogResponse;
import com.salah.booknest.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("")
    @PreAuthorize("hasRole('librarian')")
    public List<AuditLogResponse> getAuditLogs(){
        return auditLogService.getAuditLogs();
    }
}
