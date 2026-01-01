package net.javaguides.identity_service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import net.javaguides.identity_service.entity.AuditLog;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AuditLogDto {

    private Long id;
    private String username;
    private String action;
    private String resource;
    private LocalDateTime createdAt;

    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getUsername(),
                log.getAction(),
                log.getResource(),
                log.getCreatedAt()
        );
    }
}
