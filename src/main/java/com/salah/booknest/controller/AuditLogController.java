package com.salah.booknest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.salah.booknest.model.response.AuditLogResponse;
import com.salah.booknest.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit log", description = "Trail of who did what to which record. Librarian only.")
@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "List audit entries (paged)", description = "Newest first by default. Filter by entry type (USER, BOOK, LOAN or REVIEW) and/or by the id of the user who acted. Sort by id, timestamp, type, userId or action. Page size is capped at 100. Librarian only.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Unknown sortBy field")
    })
    @GetMapping("")
    @PreAuthorize("hasRole('librarian')")
    public Page<AuditLogResponse> getAuditLogs(
            @Parameter(description = "USER, BOOK, LOAN or REVIEW") @RequestParam(required = false) String type,
            @Parameter(description = "Id of the user who performed the action") @RequestParam(required = false) Long userId,
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Entries per page (1 to 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id, timestamp, type, userId or action") @RequestParam(defaultValue = "timestamp") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "desc") String sortDir) {
        return auditLogService.getAuditLogs(type, userId, page, size, sortBy, sortDir);
    }
}
