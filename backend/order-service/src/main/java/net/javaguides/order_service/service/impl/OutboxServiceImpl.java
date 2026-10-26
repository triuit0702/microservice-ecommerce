package net.javaguides.order_service.service.impl;

import net.javaguides.order_service.entity.OutboxEvent;
import net.javaguides.order_service.repository.OutboxRepository;
import net.javaguides.order_service.service.OutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class OutboxServiceImpl implements OutboxService {

    @Autowired
    private OutboxRepository outboxRepository;

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return outboxRepository.findPending(PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public void markPublishedById(Long id, LocalDateTime publishedAt) {
        outboxRepository.markPublishedById(id, publishedAt);
    }
}
