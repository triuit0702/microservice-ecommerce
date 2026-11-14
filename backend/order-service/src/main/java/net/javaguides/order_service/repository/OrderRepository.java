package net.javaguides.order_service.repository;

import net.javaguides.order_service.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;
import java.util.List;
import java.util.Optional;


public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByUserId(@PathVariable("userId") Long userId, Pageable pageable);

    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Query("SELECT o.orderId FROM Order o WHERE o.status = :status AND o.createdAt < :before ORDER BY o.createdAt ASC")
    List<String> findIdsByStatusCreatedBefore(@Param("status") String status, @Param("before") Date before,
                                             Pageable pageable);
}
