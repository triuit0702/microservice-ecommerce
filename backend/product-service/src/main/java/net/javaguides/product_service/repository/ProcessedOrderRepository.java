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

    /**
     * Switch RESERVED to RELEASED. The status check in the WHERE clause makes a release happen at most once.
     * @return 1 if the order was RESERVED, 0 otherwise (never reserved, cancelled or already released)
     */
    @Modifying
    @Query("""
    UPDATE ProcessedOrder p SET p.status = net.javaguides.common_lib.enums.InventoryStatus.RELEASED
    WHERE p.orderId = :orderId AND p.status = net.javaguides.common_lib.enums.InventoryStatus.RESERVED
    """)
    int markReleased(@Param("orderId") String orderId);
}
