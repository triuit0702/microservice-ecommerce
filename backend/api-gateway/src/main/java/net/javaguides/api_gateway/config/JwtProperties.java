package net.javaguides.api_gateway.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;

/**
 * JWT settings (prefix "jwt"). The secret must be the same as in identity-service.
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** Base64-encoded HMAC secret, from env var JWT_SECRET. */
    private String secret;
    private String issuer = "identity-service";

    public SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
