package net.javaguides.identity_service.controller;


import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.audit.AuditLogService;
import net.javaguides.identity_service.dto.AuditLogDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    //@PreAuthorize("hasAuthority('AUDIT_VIEW')")
    @GetMapping("/recent")
    public List<AuditLogDto> recent(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return auditLogService.getRecent(limit)
                .stream()
                .map(AuditLogDto::from)
                .toList();
    }
}
