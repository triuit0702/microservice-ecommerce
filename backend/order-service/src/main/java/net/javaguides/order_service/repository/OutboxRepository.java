package net.javaguides.order_service.repository;

import net.javaguides.order_service.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 0 ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPending(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 1, e.publishedAt = :publishedAt WHERE e.id = :id")
    void markPublishedById(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt);
}
