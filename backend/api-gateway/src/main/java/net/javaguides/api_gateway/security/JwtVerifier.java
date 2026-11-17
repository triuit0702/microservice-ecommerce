package net.javaguides.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import net.javaguides.api_gateway.config.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifies access tokens (signature, expiry, issuer) and maps claims to {@link AuthUser}.
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtVerifier {
    private final SecretKey key;
    private final String issuer;

    public JwtVerifier(JwtProperties jwtProperties) {
        this.key = jwtProperties.signingKey();
        this.issuer = jwtProperties.getIssuer();
    }

    /**
     * @throws JwtException             if the token is invalid or expired
     * @throws IllegalArgumentException if a claim has an unexpected format (e.g. token from the old format)
     */
    public AuthUser verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthUser(
                Long.valueOf(claims.getSubject()),
                claims.get(JwtClaims.EMAIL, String.class),
                toStringSet(claims.get(JwtClaims.ROLES)));
    }

    private static Set<String> toStringSet(Object claim) {
        if (claim instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }
}
