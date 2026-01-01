package net.javaguides.identity_service.repository;

import net.javaguides.identity_service.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop10ByOrderByCreatedAtDesc();

    @Query("""
        SELECT a FROM AuditLog a
        ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> findRecent(Pageable pageable);
}
