package net.javaguides.order_service.kafka;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OrderProducer {
    private static final long ACK_TIMEOUT_SECONDS = 5;

    private final NewTopic topic;
    private final KafkaTemplate<String, OrderMessageEvent> kafkaTemplate;

    /**
     * Send the event and block until the broker acknowledges it.
     *
     * @param key        Kafka key (orderId): events of the same order go to the same partition, keeping their order
     * @param orderEvent event to send
     */
    public void sendAndAwaitAck(String key, OrderMessageEvent orderEvent) throws Exception {
        kafkaTemplate.send(topic.name(), key, orderEvent)
                .get(ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
