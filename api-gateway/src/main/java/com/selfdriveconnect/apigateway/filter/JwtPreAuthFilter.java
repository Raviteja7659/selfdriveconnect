package com.selfdriveconnect.apigateway.filter;

import com.selfdriveconnect.apigateway.security.JwtValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Mirrors the public/protected split already enforced individually by
 * vehicle-service, booking-service, and review-service, so a request
 * with no token (or a garbage one) gets rejected here instead of
 * making a wasted hop to a downstream service. The downstream services
 * keep their own identical checks — this is a fast-fail optimization
 * and a single place to reason about "what's public", not the sole
 * line of defense.
 */
@Component
@RequiredArgsConstructor
public class JwtPreAuthFilter implements GlobalFilter, Ordered {

    private final JwtValidator jwtValidator;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private record PublicRoute(HttpMethod method, String pattern) {
    }

    // Kept in sync with each service's own SecurityConfig permitAll rules.
    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(null, "/api/v1/auth/**"),                      // register/login/bootstrap-admin
            new PublicRoute(HttpMethod.GET, "/api/v1/vehicles/**"),        // browsing listings
            new PublicRoute(HttpMethod.GET, "/api/v1/bookings/availability"),
            new PublicRoute(HttpMethod.PATCH, "/api/v1/bookings/*/internal/confirm-after-payment"),
            new PublicRoute(HttpMethod.GET, "/api/v1/reviews/**"),         // browsing reviews
            new PublicRoute(null, "/actuator/**")
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        if (isPublic(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, "Missing bearer token");
        }

        String token = authHeader.substring(7);
        if (!jwtValidator.isValid(token)) {
            return reject(exchange, "Invalid or expired token");
        }

        // Valid token — forward the request WITH its original
        // Authorization header intact. Downstream services parse it
        // themselves to get userId/roles and enforce ownership; the
        // gateway never strips or rewrites it.
        return chain.filter(exchange);
    }

    private boolean isPublic(String path, HttpMethod method) {
        return PUBLIC_ROUTES.stream().anyMatch(route ->
                (route.method() == null || route.method().equals(method))
                        && pathMatcher.match(route.pattern(), path));
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
