package net.javaguides.product_service.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topics this service writes to. Declared here so KafkaAdmin creates them on startup;
 * names are read from {@link KafkaTopicsProperties}, never from these beans.
 */
@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private final KafkaTopicsProperties topics;

    /** Reservation results sent to order-service. */
    @Bean
    NewTopic inventoryResultTopic() {
        return TopicBuilder.name(topics.inventoryResult()).partitions(topics.partitions()).build();
    }

    /**
     * Failed inventory commands, written by this service's error handler.
     * Must have the same partition count as inventory.command (declared by order-service).
     */
    @Bean
    NewTopic inventoryCommandDltTopic() {
        return TopicBuilder.name(topics.inventoryCommandDlt()).partitions(topics.partitions()).build();
    }
}
