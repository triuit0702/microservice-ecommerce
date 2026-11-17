package net.javaguides.identity_service.security;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.config.JwtProperties;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * Signs access tokens. Verification is done by {@link JwtVerifier}.
 */
@Component
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    public String generateAccessToken(AuthUser user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim(JwtClaims.USERNAME, user.username())
                .claim(JwtClaims.EMAIL, user.email())
                .claim(JwtClaims.ROLES, user.roles())
                .claim(JwtClaims.PERMISSIONS, user.permissions())
                .issuer(jwtProperties.getIssuer())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getAccessTtl().toMillis()))
                .signWith(jwtProperties.signingKey(), Jwts.SIG.HS256)
                .compact();
    }
}
