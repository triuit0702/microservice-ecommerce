package net.javaguides.identity_service.service;

import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.dto.cache.UserCache;
import net.javaguides.identity_service.security.AuthUser;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;

/**
 * Redis cache of /auth/me data. Evicted when a user's name, email or roles change.
 */
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private static final String KEY_PREFIX = "user:";
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    private String buildKey(Long userId) {
        return KEY_PREFIX + userId;
    }

    public void cacheUser(UserCache userCache) {
        redisTemplate.opsForValue().set(buildKey(userCache.getId()), userCache, USER_CACHE_TTL);
    }

    /**
     * @return the cached user, or null if not found
     */
    public UserCache getCachedUser(Long userId) {
        return (UserCache) redisTemplate.opsForValue().get(buildKey(userId));
    }

    public void evictUser(Long userId) {
        redisTemplate.delete(buildKey(userId));
    }

    public UserCache buildUserCache(AuthUser user) {
        // copy to HashSet: the JSON serializer stores the concrete type, immutable sets cannot be read back
        return new UserCache(
                user.id(),
                user.username(),
                user.email(),
                new HashSet<>(user.roles()),
                new HashSet<>(user.permissions())
        );
    }
}
