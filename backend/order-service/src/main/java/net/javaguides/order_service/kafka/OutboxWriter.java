package net.javaguides.order_service.kafka;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.javaguides.order_service.entity.OutboxEvent;
import net.javaguides.order_service.repository.OutboxRepository;
import net.javaguides.order_service.utils.Constants;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxRepository repo;
    private final ObjectMapper mapper;


    public void save(String topic, String aggregateId, Object payload) {
        try {
            repo.save(OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .payload(mapper.writeValueAsString(payload))
                    .status(Constants.OUTBOX_STATUS_NEW)
                    .createdAt(Instant.now())
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);   // runtime để rollback cả đơn
        }
    }
}
