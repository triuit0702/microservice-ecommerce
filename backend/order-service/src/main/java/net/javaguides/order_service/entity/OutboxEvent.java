package net.javaguides.order_service.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;


@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_status_created", columnList = "status, created_at")
})

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String aggregateId;             // orderId, dùng làm key Kafka

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;


    private int status;            // NEW: 0 | SENT : 1
    private Instant createdAt;
    private LocalDateTime publishedAt;

}
