package net.javaguides.order_service.config;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Error handling of Kafka listeners (picked up by Spring Boot's listener container factory).
 * A record that keeps failing (or cannot be deserialized) is moved to the inventory result DLT
 * (declared in {@link KafkaTopicConfig}) so it never blocks the records behind it on the same partition.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);

    // Retries for transient errors (e.g. DB down, optimistic lock conflict); deserialization errors are never retried
    @Value("${order.kafka.retry.interval-ms:1000}")
    private long retryIntervalMs;

    @Value("${order.kafka.retry.max-attempts:3}")
    private long retryMaxAttempts;

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaProperties kafkaProperties, KafkaTopicsProperties topics) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                deadLetterTemplate(kafkaProperties),
                // The only listener consumes inventory results; the DLT has the same partition count, so the record
                // keeps its partition (and per-order ordering)
                (consumerRecord, ex) -> {
                    LOGGER.error("Kafka record {}-{}@{} moved to DLT: {}", consumerRecord.topic(),
                            consumerRecord.partition(), consumerRecord.offset(), ex.getMessage());
                    return new TopicPartition(topics.inventoryResultDlt(), consumerRecord.partition());
                });
        return new DefaultErrorHandler(recoverer, new FixedBackOff(retryIntervalMs, retryMaxAttempts));
    }

    /**
     * Template used only by the recoverer (not a bean, so Boot's KafkaTemplate used by OrderProducer is kept).
     * Undeserializable records arrive as raw byte[]; other failed records are re-serialized as JSON.
     */
    private KafkaTemplate<Object, Object> deadLetterTemplate(KafkaProperties kafkaProperties) {
        Map<Class<?>, Serializer<?>> valueSerializers = new LinkedHashMap<>();
        valueSerializers.put(byte[].class, new ByteArraySerializer());
        valueSerializers.put(Object.class, new JsonSerializer<>());

        Map<String, Object> producerProps = new LinkedHashMap<>(kafkaProperties.buildProducerProperties(null));
        // Keys are always String (StringDeserializer on the consumer side)
        Serializer<Object> keySerializer = new DelegatingByTypeSerializer(Map.of(String.class, new StringSerializer()));
        Serializer<Object> valueSerializer = new DelegatingByTypeSerializer(valueSerializers, true);
        DefaultKafkaProducerFactory<Object, Object> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps, keySerializer, valueSerializer);
        return new KafkaTemplate<>(producerFactory);
    }
}
