package net.javaguides.product_service.repository;

import net.javaguides.product_service.entity.ProcessedOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedOrderRepository extends JpaRepository<ProcessedOrder, String> {

    @Modifying
    @Query("UPDATE ProcessedOrder p SET p.published = true WHERE p.orderId = :orderId")
    int markPublished(@Param("orderId") String orderId);
}
