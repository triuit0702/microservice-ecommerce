package net.javaguides.identity_service.audit;


import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.entity.AuditLog;
import net.javaguides.identity_service.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void save(String username, String action, String resource) {
        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction(action);
        log.setResource(resource);
        auditLogRepository.save(log);
    }

    public List<AuditLog> getRecent(int limit) {
        return auditLogRepository
                .findRecent(PageRequest.of(0, limit))
                .getContent();
    }
}
