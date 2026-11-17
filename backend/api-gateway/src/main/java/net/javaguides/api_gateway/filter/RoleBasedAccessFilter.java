package net.javaguides.api_gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.javaguides.api_gateway.security.AuthUser;
import net.javaguides.common_lib.dto.ApiResponse;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Route filter: coarse role check (requiredRoles in YAML). Must be declared after AuthenticationFilter,
 * it reuses the AuthUser parsed there. Fine-grained permissions are checked in the services.
 */
@Component
public class RoleBasedAccessFilter extends AbstractGatewayFilterFactory<RoleBasedAccessFilter.Config> {

    private final ObjectMapper objectMapper;

    public RoleBasedAccessFilter(ObjectMapper objectMapper) {
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            AuthUser user = exchange.getAttribute(AuthUser.ATTRIBUTE);
            if (user == null) {
                return onError(exchange, "Unauthorized access", HttpStatus.UNAUTHORIZED);
            }

            List<String> requiredRoles = config.getRequiredRoles();
            boolean hasRole = requiredRoles.isEmpty() || requiredRoles.stream().anyMatch(user.roles()::contains);
            if (!hasRole) {
                return onError(exchange, "You don't have the required role to access this resource.", HttpStatus.FORBIDDEN);
            }
            return chain.filter(exchange);
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
        private List<String> requiredRoles;

        public List<String> getRequiredRoles() {
            return requiredRoles != null ? requiredRoles : Collections.emptyList();
        }

        public void setRequiredRoles(List<String> requiredRoles) {
            this.requiredRoles = requiredRoles == null ? null : requiredRoles.stream().map(String::trim).toList();
        }
    }
}
