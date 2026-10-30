package net.javaguides.order_service.kafka;

import net.javaguides.common_lib.dto.order.OrderMessageEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OrderProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProducer.class);
    private NewTopic topic;
    private KafkaTemplate<String, OrderMessageEvent> kafkaTemplate;

    public OrderProducer(NewTopic topic, KafkaTemplate<String, OrderMessageEvent> kafkaTemplate) {
        this.topic = topic;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(OrderMessageEvent orderEvent){
        LOGGER.info(String.format("OrderDTO event => %s", orderEvent.toString()));


        //create message
        Message<OrderMessageEvent> message = MessageBuilder.withPayload(orderEvent)
                .setHeader(KafkaHeaders.TOPIC, topic.name())
                .build();
        //kafkaTemplate.send(message);

        kafkaTemplate.send(topic.name(), orderEvent)
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

    public void sendAndAwaitAck(OrderMessageEvent orderEvent) throws Exception {
            kafkaTemplate.send(topic.name(), orderEvent)
                    .get(5, TimeUnit.SECONDS); // // chờ Broker ACK, timeout sau 5s

    }


}
