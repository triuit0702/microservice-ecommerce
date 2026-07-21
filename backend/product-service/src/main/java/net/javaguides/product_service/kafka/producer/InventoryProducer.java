package net.javaguides.product_service.kafka.producer;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.order.OrderDTO;
import net.javaguides.common_lib.dto.order.OrderEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


@Component
@RequiredArgsConstructor
public class InventoryProducer {

    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;

    @Value("${spring.kafka.update-order-topic.name}")
    private String updateOrderTopic;

    public void sendOrderStatusUpdate(OrderDTO orderDTO)  {
        kafkaTemplate.send(updateOrderTopic, orderDTO);
    }
}
