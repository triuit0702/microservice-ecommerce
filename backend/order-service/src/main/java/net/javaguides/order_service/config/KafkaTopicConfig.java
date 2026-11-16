package net.javaguides.order_service.config;

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
class KafkaTopicConfig {

    private final KafkaTopicsProperties topics;

    /** Commands sent to product-service. */
    @Bean
    NewTopic inventoryCommandTopic() {
        return TopicBuilder.name(topics.inventoryCommand()).partitions(topics.partitions()).build();
    }

    /** Failed inventory results, written by this service's error handler (same partition count as the source). */
    @Bean
    NewTopic inventoryResultDltTopic() {
        return TopicBuilder.name(topics.inventoryResultDlt()).partitions(topics.partitions()).build();
    }
}
