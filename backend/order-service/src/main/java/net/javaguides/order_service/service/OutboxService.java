package net.javaguides.order_service.service;

import net.javaguides.order_service.entity.OutboxEvent;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxService {
    List<OutboxEvent> findPending(int limit);

    void markPublishedById( Long id, LocalDateTime publishedAt);
}
