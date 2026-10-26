package net.javaguides.product_service.kafka.producer;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class InventoryProducer {

    private final KafkaTemplate<String, OrderMessageEvent> kafkaTemplate;

    @Value("${spring.kafka.update-order-topic.name}")
    private String updateOrderTopic;

    public void sendOrderStatusUpdate(OrderMessageEvent orderMessageEvent)  {
        kafkaTemplate.send(updateOrderTopic, orderMessageEvent);
    }
}
