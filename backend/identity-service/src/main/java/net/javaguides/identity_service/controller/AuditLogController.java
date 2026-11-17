package net.javaguides.identity_service.controller;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.identity_service.audit.AuditLogService;
import net.javaguides.identity_service.dto.AuditLogDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Audit log of user management actions. Same permission as viewing users (ADMINISTRATOR in init.sql).
 */
@RestController
@RequestMapping("/api/v1/user/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PreAuthorize("hasAuthority('user:read')")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> recent(
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<AuditLogDto> logs = auditLogService.getRecent(limit)
                .stream()
                .map(AuditLogDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
