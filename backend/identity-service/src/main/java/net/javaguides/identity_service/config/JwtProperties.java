package net.javaguides.identity_service.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import java.time.Duration;

/**
 * JWT and auth cookie settings (prefix "jwt"). The secret must be the same as in api-gateway.
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** Base64-encoded HMAC secret, from env var JWT_SECRET. */
    private String secret;
    private String issuer = "identity-service";
    private Duration accessTtl = Duration.ofMinutes(15);
    private Duration refreshTtl = Duration.ofDays(7);
    /** Secure flag of auth cookies: true in production (https). */
    private boolean cookieSecure = false;

    public SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
