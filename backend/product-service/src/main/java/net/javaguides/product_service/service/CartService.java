package net.javaguides.product_service.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;

    private String buildKey(Long userId) {
        return "cart:" + userId;
    }

    public void addToCart(Long userId, String productId, Integer quantity) {

        String key = buildKey(userId);
        // increment() là atomic → không sợ spam add
        redisTemplate.opsForHash()
                .increment(key, productId, quantity);
        // TTL 7 ngày → tự xoá cart cũ
        redisTemplate.expire(key, Duration.ofDays(7));
    }

    public Map<Object, Object> getCart(Long userId) {
        return redisTemplate.opsForHash().entries(buildKey(userId));
    }

    public void removeItem(Long userId, Long productId) {
        redisTemplate.opsForHash()
                .delete(buildKey(userId), productId.toString());
    }

    public void clearCart(Long userId) {
        redisTemplate.delete(buildKey(userId));
    }
}
