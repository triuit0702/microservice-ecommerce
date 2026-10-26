package net.javaguides.order_service.cronjob;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import net.javaguides.order_service.entity.OutboxEvent;
import net.javaguides.order_service.kafka.OrderProducer;
import net.javaguides.order_service.service.OutboxService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherJob {

    private static final int BATCH_SIZE = 500;
    private final OutboxService outboxService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final OrderProducer orderProducer;


    // fixedDelay: chờ 1s sau khi lần trước kết thúc, không overlap
    @Scheduled(fixedDelay = 1000)
    public void publish() {
        log.debug("OutboxPublisher START JOB ");
        List<OutboxEvent> events = outboxService.findPending(BATCH_SIZE);

        log.debug("OutboxPublisher [row-by-row]: found {} PENDING events", events.size());

        for (OutboxEvent event : events) {
           try {

               OrderMessageEvent orderEvent = objectMapper.readValue(event.getPayload(), OrderMessageEvent.class);
               // Gửi và CHỜ Broker ACK (blocking) — chỉ sau khi Broker xác nhận mới update DB
               orderProducer.sendAndAwaitAck(orderEvent);

               // ack thành công => update trạng thái PUBLISHED, nó sẽ được loại ra khỏi batch lần sau
                outboxService.markPublishedById(event.getId(), event.getPublishedAt());

               log.debug("OutboxPublisher [row-by-row]: published eventId={} ",
                       event.getId());
           } catch (Exception e) {
               log.error("Failed to publish event with ID {}: {}", event.getId(), e);
           }
        }
    }
}
