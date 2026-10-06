package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.model.response.AuditLogResponse;
import com.salah.booknest.service.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Audit log", description = "Trail of who did what to which record. Librarian only.")
@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "List audit entries", description = "Newest first. Filter by entry type (USER, BOOK, LOAN or REVIEW) and/or by the id of the user who acted. Librarian only.")
    @GetMapping("")
    @PreAuthorize("hasRole('librarian')")
    public List<AuditLogResponse> getAuditLogs(@Parameter(description = "USER, BOOK, LOAN or REVIEW") @RequestParam(required = false) String type,
                                               @Parameter(description = "Id of the user who performed the action") @RequestParam(required = false) Long userId) {
        return auditLogService.getAuditLogs(type, userId);
    }
}
