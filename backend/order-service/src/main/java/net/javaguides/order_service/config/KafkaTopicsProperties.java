package net.javaguides.order_service.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Kafka topic names, shared by producers, listeners and topic declarations.
 *
 * @param inventoryCommand commands to product-service (CREATE_ORDER, RELEASE_STOCK), owned by order-service
 * @param inventoryResult  reservation results from product-service (RESERVED, CANCEL), owned by product-service
 * @param partitions       partition count of the topics declared by this service
 */
@Validated
@ConfigurationProperties("app.kafka.topics")
public record KafkaTopicsProperties(
        @NotBlank String inventoryCommand,
        @NotBlank String inventoryResult,
        @Min(1) int partitions) {

    /** Dead-letter topic of inventory results that keep failing in this service. */
    public String inventoryResultDlt() {
        return inventoryResult + ".DLT";
    }
}
