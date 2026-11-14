package net.javaguides.order_service.cronjob;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javaguides.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * Cancels orders that stay PENDING too long (no inventory result received) and requests a stock release,
 * so an order never stays PENDING forever and stock reserved for it is given back.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingOrderTimeoutJob {

    private static final int BATCH_SIZE = 100;
    static final String TIMEOUT_REASON = "Order could not be processed in time, please try again";

    private final OrderService orderService;

    @Value("${order.pending-timeout-minutes:5}")
    private long pendingTimeoutMinutes;

    @Scheduled(fixedDelayString = "${order.pending-timeout-check-interval-ms:60000}")
    public void cancelExpiredOrders() {
        Date createdBefore = new Date(System.currentTimeMillis() - Duration.ofMinutes(pendingTimeoutMinutes).toMillis());
        List<String> orderIds = orderService.findExpiredPendingOrderIds(createdBefore, BATCH_SIZE);
        if (orderIds.isEmpty()) {
            return;
        }
        log.info("PendingOrderTimeout: found {} orders PENDING for more than {} minutes", orderIds.size(), pendingTimeoutMinutes);

        for (String orderId : orderIds) {
            try {
                orderService.cancelExpiredOrder(orderId, TIMEOUT_REASON);
            } catch (ObjectOptimisticLockingFailureException e) {
                // The inventory result updated the order at the same time: it wins, nothing to do
                log.info("Order {} was updated concurrently, skip timeout cancel", orderId);
            } catch (Exception e) {
                // One bad order must not stop the batch; it is picked up again on the next run
                log.error("Failed to cancel expired order {}", orderId, e);
            }
        }
    }
}
