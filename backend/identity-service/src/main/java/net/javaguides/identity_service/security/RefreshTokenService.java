package net.javaguides.identity_service.security;

import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.config.JwtProperties;
import net.javaguides.identity_service.exception.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;

/**
 * Opaque refresh tokens stored in Redis by SHA-256 hash, with rotation and reuse detection.
 * <ul>
 *   <li>refresh:{hash} -> userId (active token)</li>
 *   <li>refresh:used:{hash} -> userId (already rotated, presenting it again revokes all tokens of the user)</li>
 *   <li>refresh:user:{userId} -> set of active hashes (to revoke all)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenService.class);

    private static final String TOKEN_KEY = "refresh:";
    private static final String USED_KEY = "refresh:used:";
    private static final String USER_KEY = "refresh:user:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Create a new refresh token for the user and return its raw value (only sent in the cookie).
     */
    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = hash(rawToken);
        String userKey = USER_KEY + userId;

        redisTemplate.opsForValue().set(TOKEN_KEY + hash, String.valueOf(userId), jwtProperties.getRefreshTtl());
        redisTemplate.opsForSet().add(userKey, hash);
        redisTemplate.expire(userKey, jwtProperties.getRefreshTtl());
        return rawToken;
    }

    /**
     * Consume a refresh token and return its user id. The caller must issue a new one.
     *
     * @throws AuthException if the token is unknown, expired or already used
     */
    public Long rotate(String rawToken) {
        String hash = hash(rawToken);
        String userId = redisTemplate.opsForValue().getAndDelete(TOKEN_KEY + hash);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_KEY + userId, hash);
            redisTemplate.opsForValue().set(USED_KEY + hash, userId, jwtProperties.getRefreshTtl());
            return Long.valueOf(userId);
        }

        String reusedBy = redisTemplate.opsForValue().get(USED_KEY + hash);
        if (reusedBy != null) {
            LOGGER.warn("Refresh token reuse detected for userId {}, revoking all refresh tokens", reusedBy);
            revokeAll(Long.valueOf(reusedBy));
        }
        throw new AuthException("Invalid or expired refresh token");
    }

    /**
     * Revoke one refresh token (logout). Unknown tokens are ignored.
     */
    public void revoke(String rawToken) {
        String hash = hash(rawToken);
        String userId = redisTemplate.opsForValue().getAndDelete(TOKEN_KEY + hash);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_KEY + userId, hash);
        }
    }

    /**
     * Revoke every refresh token of the user, e.g. after a role change or delete.
     */
    public void revokeAll(Long userId) {
        String userKey = USER_KEY + userId;
        Set<String> hashes = redisTemplate.opsForSet().members(userKey);
        if (hashes != null && !hashes.isEmpty()) {
            redisTemplate.delete(hashes.stream().map(hash -> TOKEN_KEY + hash).toList());
        }
        redisTemplate.delete(userKey);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
