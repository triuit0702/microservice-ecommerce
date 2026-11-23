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

    private static final String REFRESH_TOKEN_KEY = "refresh:";
    private static final String USED_REFRESH_TOKEN_KEY = "refresh:used:";
    private static final String USER_REFRESH_TOKENS_KEY = "refresh:user:";

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
        String userRefreshTokensKey = USER_REFRESH_TOKENS_KEY + userId;

        redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY + hash, String.valueOf(userId), jwtProperties.getRefreshTtl());
        redisTemplate.opsForSet().add(userRefreshTokensKey, hash);
        redisTemplate.expire(userRefreshTokensKey, jwtProperties.getRefreshTtl());
        return rawToken;
    }

    /**
     * Consume a refresh token and return its user id. The caller must issue a new one.
     *
     * @throws AuthException if the token is unknown, expired or already used
     */
    public Long rotate(String rawToken) {
        // 1. Hash the raw cookie value: Redis only stores hashes, never the raw token
        String hash = hash(rawToken);

        // 2. Read and delete the active token in one atomic step, so two concurrent
        //    refresh calls with the same token cannot both succeed
        String userId = redisTemplate.opsForValue().getAndDelete(REFRESH_TOKEN_KEY + hash);
        if (userId != null) {
            // 3. Valid token: remove it from the user's token set (no longer needed by revokeAll)
            redisTemplate.opsForSet().remove(USER_REFRESH_TOKENS_KEY + userId, hash);
            // 4. Remember it as used, so presenting it again is detected as reuse
            redisTemplate.opsForValue().set(USED_REFRESH_TOKEN_KEY + hash, userId, jwtProperties.getRefreshTtl());
            // 5. Return the user id; the caller issues a new access token and refresh token
            return Long.valueOf(userId);
        }

        // 6. Not an active token: check if it was already rotated before
        String reusedBy = redisTemplate.opsForValue().get(USED_REFRESH_TOKEN_KEY + hash);
        if (reusedBy != null) {
            // 7. Reuse of an old token means it was probably stolen: revoke all tokens of the user,
            //    so both the attacker and the real user must log in again
            LOGGER.warn("Refresh token reuse detected for userId {}, revoking all refresh tokens", reusedBy);
            revokeAll(Long.valueOf(reusedBy));
        }
        // 8. Unknown, expired or reused token: reject (the caller clears the auth cookies)
        throw new AuthException("Invalid or expired refresh token");
    }

    /**
     * Revoke one refresh token (logout). Unknown tokens are ignored.
     */
    public void revoke(String rawToken) {
        String hash = hash(rawToken);
        String userId = redisTemplate.opsForValue().getAndDelete(REFRESH_TOKEN_KEY + hash);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_REFRESH_TOKENS_KEY + userId, hash);
        }
    }

    /**
     * Revoke every refresh token of the user, e.g. after a role change or delete.
     */
    public void revokeAll(Long userId) {
        String userRefreshTokensKey = USER_REFRESH_TOKENS_KEY + userId;
        Set<String> hashes = redisTemplate.opsForSet().members(userRefreshTokensKey);
        if (hashes != null && !hashes.isEmpty()) {
            redisTemplate.delete(hashes.stream().map(hash -> REFRESH_TOKEN_KEY + hash).toList());
        }
        redisTemplate.delete(userRefreshTokensKey);
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
