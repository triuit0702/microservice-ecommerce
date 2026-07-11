package net.javaguides.product_service.service;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.javaguides.product_service.dto.cart.AddCartRequestDto;
import net.javaguides.product_service.dto.cart.CartItemDto;
import net.javaguides.product_service.dto.cart.CartResponseDto;
import net.javaguides.product_service.entity.Product;
import net.javaguides.product_service.entity.ProductVariant;
import net.javaguides.product_service.projection.CartItemView;
import net.javaguides.product_service.repository.ProductVariantRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final ProductService productService;
    private final ProductVariantRepository productVariantRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private String buildKey(Long userId) {
        return "cart:" + userId;
    }

    /**
     * add to cart
     * key: cart:userId
     * field: productId:variantId
     * value: quantity
     * @param request
     */
    public void addToCart(AddCartRequestDto request) {
        // lưu stock vào redis khi lần đầu add vào cart
        addStockToCache(request.getProductId(), request.getVariantId());

        String key = buildKey(request.getUserId());
        String field = request.getProductId()
                + ":" + request.getVariantId();

        // increment() là atomic → không sợ spam add
        redisTemplate.opsForHash()
                .increment(key, field, request.getQuantity());
        // TTL 7 ngày → tự xoá cart cũ
        //redisTemplate.expire(key, Duration.ofDays(7));
    }

    /**
     * remove cart item selected
     * @param request
     */
    public void removeCartItemSelected(AddCartRequestDto request) {
        String key = buildKey(request.getUserId());
        String field = request.getProductId()
                + ":" + request.getVariantId();

        // remove item from redis hash
        redisTemplate.opsForHash()
                .delete(key, field);
    }

    // add stock to redis cache
    private void addStockToCache(String productId, String variantId) {
        String stockey = "stock:" + productId + ":" + variantId;
        if (redisTemplate.hasKey(stockey)) {
            return;
        }
        Optional<Integer> variant = productVariantRepository.findById(Long.valueOf(variantId)).map(ProductVariant::getStockQuantity);
        if (variant.isEmpty()) {
            return;
        }
        redisTemplate.opsForValue().set(stockey, variant.orElse(0));

    }

    public CartResponseDto getCart(Long userId) {
        // get key : cart:UserId
        String key = buildKey(userId);



        // get all entries in hash redis by key
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(buildKey(userId));
        if (entries.isEmpty()) {
            return new CartResponseDto(userId.toString(), List.of());
        }

        // cau truc cua redis: key = cart:userId, field = productId:variantId, value = quantity
        // parse redis data
        List<CartItemDto> items = entries.entrySet().stream()
                .map(e -> {
                    // tách productId, variantId, color từ key
                    String[] parts = e.getKey().toString().split(":");
                    String productId = parts[0];
                    Long variantId = Long.parseLong(parts[1]);
                    Integer quantity = Integer.parseInt(e.getValue().toString());

                    CartItemDto dto = new CartItemDto();
                    dto.setProductId(productId);
                    dto.setVariantId(variantId);
                    dto.setQuantity(quantity);
                    return dto;
                })
                .toList();

        // get list variant id
        List<Long> variantIdList = items.stream()
                .map(CartItemDto::getVariantId)
                .distinct()
                .toList();

        // get list cart item view from db
        List<CartItemView> cartItemViewList = productVariantRepository.getCartItems(variantIdList);

        Map<Long, CartItemView> cartItemMap = cartItemViewList.stream()
                .collect(Collectors.toMap(CartItemView::getVariantId, p -> p));

        // merge data
        items.forEach(item -> {
            CartItemView v  = cartItemMap.get(item.getVariantId());
            if (v != null) {
                item.setName(v.getProductName());
                item.setImageUrl(v.getImageUrl());
                item.setPrice(v.getPrice());
                item.setColor(v.getColor());
            }
        });

        return new CartResponseDto(userId.toString(), items);
    }

    public void removeItem(Long userId, Long productId, String variantId) {
        String field = productId + ":" + variantId;
        redisTemplate.opsForHash()
                .delete(buildKey(userId), field);
    }

//    public void clearCart(Long userId) {
//        redisTemplate.delete(buildKey(userId));
//    }

    public int updateQuantity(AddCartRequestDto request) {
        // dùng lua script để update quantity trong giỏ hàng
        String key = buildKey(request.getUserId());
        String field = request.getProductId() + ":" + request.getVariantId();
//        String luaScript = "local userId=KEYS[1]" +
//                "local field = KEYS[3]" +
//                "local newQty = tonumber(ARGV[1]) " +
//                "local stockCurr = tonumber(ARGV[2])" +
//                "if newQty > stockCurr then " +
//                "   newQty = stockCurr " +
//                "elseif newQty <= 0" +
//                "   redis.call("HDEL", "cart:" ..userId)"
//                ;

        String luaScript = """
            
            local userId = KEYS[1]
            local field = KEYS[2]
            local newQty = tonumber(ARGV[1])
            local stock = tonumber(ARGV[2])
            
            if newQty > stock then
                newQty = stock
            elseif newQty <= 0 then
                redis.call("HDEL", "cart:" .. userId, field)
                return 0
            end
            
            redis.call("HSET", "cart:" .. userId, field, newQty)
            return newQty
            """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);

        // KEYS
        List<String> keys = Arrays.asList(request.getUserId().toString(), request.getProductId() + ":" + request.getVariantId());


        // get current stock from redis
        //TODO
        int stock = 5;


        // ARGV
        Object[] args = new Object[]{request.getQuantity(), stock};

        //excute script
        Long result = redisTemplate.execute(redisScript, keys, args);
        return result.intValue();
    }
}
