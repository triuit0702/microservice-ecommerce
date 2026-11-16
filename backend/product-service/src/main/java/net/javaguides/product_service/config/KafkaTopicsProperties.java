package net.javaguides.product_service.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Kafka topic names, shared by producers, listeners and topic declarations.
 *
 * @param inventoryCommand commands from order-service (CREATE_ORDER, RELEASE_STOCK), owned by order-service
 * @param inventoryResult  reservation results to order-service (RESERVED, CANCEL), owned by product-service
 * @param partitions       partition count of the topics declared by this service
 */
@Validated
@ConfigurationProperties("app.kafka.topics")
public record KafkaTopicsProperties(
        @NotBlank String inventoryCommand,
        @NotBlank String inventoryResult,
        @Min(1) int partitions) {

    /** Dead-letter topic of inventory commands that keep failing in this service. */
    public String inventoryCommandDlt() {
        return inventoryCommand + ".DLT";
    }
}
