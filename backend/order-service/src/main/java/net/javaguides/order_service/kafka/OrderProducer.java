package net.javaguides.order_service.kafka;

import net.javaguides.common_lib.dto.order.OrderEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class OrderProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProducer.class);
    private NewTopic topic;
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderProducer(NewTopic topic, KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.topic = topic;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(OrderEvent orderEvent){
        LOGGER.info(String.format("OrderDTO event => %s", orderEvent.toString()));


        //create message
        Message<OrderEvent> message = MessageBuilder.withPayload(orderEvent)
                .setHeader(KafkaHeaders.TOPIC, topic.name())
                .build();
        //kafkaTemplate.send(message);

        kafkaTemplate.send("create_order_topic", orderEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        LOGGER.error("Send Kafka failed", ex);
                    } else {
                        var metadata = result.getRecordMetadata();
                        LOGGER.info("Send Kafka success: topic={}, partition={}, offset={}",
                                metadata.topic(),
                                metadata.partition(),
                                metadata.offset());
                    }
                });
        System.out.print("run kafka tset");
    }
}
