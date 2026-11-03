package net.javaguides.identity_service.service;


import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.config.CustomUserDetails;
import net.javaguides.identity_service.dto.cache.UserCache;
import net.javaguides.identity_service.entity.Permission;
import net.javaguides.identity_service.entity.UserCredential;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private static final String KEY_PREFIX = "user:";

    // Same as JWT lifetime (JwtServiceImpl): cached permissions never outlive the token.
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    private String buildKey(Long userId) {
        return KEY_PREFIX + userId;
    }

    /**
     * Cache user information in Redis with a TTL.
     *
     * @param userCache the user information to cache
     */
    public void cacheUser(UserCache userCache) {
        redisTemplate.opsForValue().set(buildKey(userCache.getId()), userCache, USER_CACHE_TTL);
    }

    /**
     * Retrieve cached user information from Redis.
     *
     * @param userId the ID of the user to retrieve
     * @return the cached user information, or null if not found
     */
    public UserCache getCachedUser(Long userId) {
        return (UserCache) redisTemplate.opsForValue().get(buildKey(userId));
    }

    /**
     * Remove cached user information, e.g. after the user's roles change or the user is deleted.
     *
     * @param userId the ID of the user to evict
     */
    public void evictUser(Long userId) {
        redisTemplate.delete(buildKey(userId));
    }

    /**
     * Build a UserCache object from a CustomUserDetails object.
     *
     * @param user the CustomUserDetails object
     * @return the UserCache object
     */
    public UserCache buildUserCache(CustomUserDetails user ) {
        return new UserCache(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPermissions()
        );
    }

    /**
     * Build a UserCache object from a UserCredential entity.
     *
     * @param user the UserCredential entity
     * @return the UserCache object
     */
    public UserCache buildUserCache(UserCredential user) {
        Set<String> permissions = user.getRoles()
                .stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        return new UserCache(
                user.getId(),
                user.getName(),
                user.getEmail(),
                permissions
        );
    }
}
