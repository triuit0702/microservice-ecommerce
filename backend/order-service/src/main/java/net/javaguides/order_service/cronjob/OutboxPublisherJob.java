package net.javaguides.order_service.cronjob;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import net.javaguides.order_service.entity.OutboxEvent;
import net.javaguides.order_service.kafka.OrderProducer;
import net.javaguides.order_service.service.OutboxService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherJob {

    private static final int BATCH_SIZE = 500;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final OrderProducer orderProducer;


    // fixedDelay: wait 1s after the previous run ends, so runs never overlap
    @Scheduled(fixedDelay = 1000)
    public void publish() {
        List<OutboxEvent> events = outboxService.findPending(BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }
        log.debug("OutboxPublisher: found {} pending events", events.size());

        for (OutboxEvent event : events) {
            OrderMessageEvent orderEvent;
            try {
                orderEvent = objectMapper.readValue(event.getPayload(), OrderMessageEvent.class);
            } catch (JsonProcessingException e) {
                // Bad payload: skip it so it does not block later events (stays NEW, needs manual fix)
                log.error("Outbox event {} has an invalid payload, skipped", event.getId(), e);
                continue;
            }

            try {
                // Block until the broker acks; only then mark the event as published
                orderProducer.sendAndAwaitAck(event.getAggregateId(), orderEvent);
                outboxService.markPublishedById(event.getId(), LocalDateTime.now());
                log.debug("OutboxPublisher: published eventId={}", event.getId());
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                // Broker likely unavailable: stop this run instead of waiting the ack timeout for every event;
                // the event stays NEW and is retried on the next run
                log.error("Failed to publish outbox event {}, will retry next run", event.getId(), e);
                break;
            }
        }
    }
}
