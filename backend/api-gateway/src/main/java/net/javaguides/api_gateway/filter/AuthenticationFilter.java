package net.javaguides.api_gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import net.javaguides.api_gateway.security.AuthUser;
import net.javaguides.api_gateway.security.JwtClaims;
import net.javaguides.api_gateway.security.JwtVerifier;
import net.javaguides.api_gateway.util.UserHeaders;
import net.javaguides.common_lib.dto.ApiResponse;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Route filter: verifies the access_token cookie once, stores the {@link AuthUser} in an exchange
 * attribute for RoleBasedAccessFilter, and sets X-User-* headers for downstream services.
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtVerifier jwtVerifier;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(JwtVerifier jwtVerifier, ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtVerifier = jwtVerifier;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            HttpCookie cookie = exchange.getRequest().getCookies().getFirst(JwtClaims.ACCESS_COOKIE);
            if (cookie == null || cookie.getValue().isBlank()) {
                return onError(exchange, "Missing access token", HttpStatus.UNAUTHORIZED);
            }

            AuthUser user;
            try {
                user = jwtVerifier.verify(cookie.getValue());
            } catch (JwtException | IllegalArgumentException e) {
                // expired or invalid: the frontend calls /auth/refresh on 401
                return onError(exchange, "Invalid or expired access token", HttpStatus.UNAUTHORIZED);
            }
            exchange.getAttributes().put(AuthUser.ATTRIBUTE, user);

            // client-sent X-User-* headers were already removed by StripUserHeadersFilter
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.set(UserHeaders.USER_ID, String.valueOf(user.id()));
                        if (user.email() != null) {
                            headers.set(UserHeaders.USER_EMAIL, user.email());
                        }
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsString(ApiResponse.error(errorMessage)).getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public static class Config {
    }
}
