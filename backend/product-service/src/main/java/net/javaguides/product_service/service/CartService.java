package net.javaguides.product_service.service;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.javaguides.product_service.dto.cart.AddCartRequestDto;
import net.javaguides.product_service.dto.cart.CartItemDto;
import net.javaguides.product_service.dto.cart.CartResponseDto;
import net.javaguides.product_service.entity.Product;
import net.javaguides.product_service.entity.ProductVariant;
import net.javaguides.product_service.exception.CartException;
import net.javaguides.product_service.projection.CartItemView;
import net.javaguides.product_service.repository.ProductVariantRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpStatus;
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

    // max number of different items (fields) in one cart
    private static final int MAX_CART_ITEMS = 200;
    private static final long RESULT_NOT_ENOUGH_STOCK = -1L;
    private static final long RESULT_CART_FULL = -2L;

    /**
     * KEYS[1] = cart:userId
     * ARGV[1] = field (productId:variantId), ARGV[2] = quantityFromRequest, ARGV[3] = quantityFromDB (stock), ARGV[4] = max items
     * return new quantity in cart, -1 if not enough stock, -2 if cart is full
     */
    private static final DefaultRedisScript<Long> ADD_TO_CART_SCRIPT = new DefaultRedisScript<>("""
            local quantityInCart = tonumber(redis.call('HGET', KEYS[1], ARGV[1]) or '0')
            local quantityFromRequest = tonumber(ARGV[2])
            local quantityFromDB = tonumber(ARGV[3])
            local maxItems = tonumber(ARGV[4])

            if quantityInCart == 0 and redis.call('HLEN', KEYS[1]) >= maxItems then
                return -2
            end
            if quantityInCart + quantityFromRequest > quantityFromDB then
                return -1
            end
            return redis.call('HINCRBY', KEYS[1], ARGV[1], quantityFromRequest)
            """, Long.class);

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
        // quantity the user wants to add (from client)
        Integer quantityFromRequest = request.getQuantity();
        if (quantityFromRequest == null || quantityFromRequest <= 0) {
            throw new CartException("Quantity must be greater than 0", HttpStatus.BAD_REQUEST);
        }

        // stock quantity of the variant (from DB, never trust client)
        Long variantId = parseVariantId(request.getVariantId());
        int quantityFromDB = productVariantRepository.findById(variantId)
                .map(ProductVariant::getStockQuantity)
                .map(s -> Math.max(s, 0))
                .orElseThrow(() -> new CartException("Product variant not found", HttpStatus.NOT_FOUND));

        String key = buildKey(request.getUserId());
        String field = request.getProductId()
                + ":" + request.getVariantId();

        // check stock + cart size and increment in one atomic script (safe against concurrent adds)
        Long result = redisTemplate.execute(
                ADD_TO_CART_SCRIPT,
                StringRedisSerializer.UTF_8,
                new GenericToStringSerializer<>(Long.class),
                List.of(key),
                field, String.valueOf(quantityFromRequest), String.valueOf(quantityFromDB), String.valueOf(MAX_CART_ITEMS));

        if (result == null) {
            throw new CartException("Unable to add item to cart", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (result == RESULT_NOT_ENOUGH_STOCK) {
            throw new CartException("Not enough stock, only " + quantityFromDB + " item(s) available", HttpStatus.BAD_REQUEST);
        }
        if (result == RESULT_CART_FULL) {
            throw new CartException("Cart is full, maximum " + MAX_CART_ITEMS + " different items", HttpStatus.BAD_REQUEST);
        }
    }

    private Long parseVariantId(String variantId) {
        try {
            return Long.valueOf(variantId);
        } catch (NumberFormatException e) {
            throw new CartException("Invalid variantId", HttpStatus.BAD_REQUEST);
        }
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
//    private void addStockToCache(String productId, String variantId) {
//        String stockey = "stock:" + productId + ":" + variantId;
//        if (redisTemplate.hasKey(stockey)) {
//            return;
//        }
//        Optional<Integer> variant = productVariantRepository.findById(Long.valueOf(variantId)).map(ProductVariant::getStockQuantity);
//        if (variant.isEmpty()) {
//            return;
//        }
//        redisTemplate.opsForValue().set(stockey, variant.orElse(0));
//
//    }

    /**
     * get cart by userId
     * @param userId
     * @return
     */
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
                    dto.setId(productId);
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


    // TODO: 
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

    /**
     * delete cart by userId
     * @param userId
     */
    public void deleteCart(Long userId) {
        redisTemplate.delete(buildKey(userId));
    }
}
