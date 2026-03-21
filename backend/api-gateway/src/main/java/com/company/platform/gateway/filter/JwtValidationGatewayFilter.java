package com.company.platform.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway-level JWT presence check. Verifies that protected routes carry an Authorization Bearer
 * header. Actual token validation is done by downstream services. This is a defense-in-depth
 * measure.
 */
@Component
public class JwtValidationGatewayFilter implements GlobalFilter, Ordered {

  private static final String BEARER_PREFIX = "Bearer ";

  private static final Set<String> PUBLIC_PATHS =
      Set.of("/api/v1/auth/login", "/api/v1/auth/refresh", "/actuator/health");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getURI().getPath();

    if (isPublicPath(path)) {
      return chain.filter(exchange);
    }

    String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader == null
        || !authHeader.startsWith(BEARER_PREFIX)
        || authHeader.length() <= BEARER_PREFIX.length()) {
      return unauthorizedResponse(exchange);
    }

    return chain.filter(exchange);
  }

  private boolean isPublicPath(String path) {
    return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
  }

  @SuppressWarnings("null") // JDT null-analysis vs Reactor Publisher<DataBuffer> bridge
  private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
    String body =
        "{\"code\":\"GATEWAY_UNAUTHORIZED\",\"message\":\"Missing or invalid authorization token\",\"details\":[],\"traceId\":\"%s\",\"timestamp\":\"%s\"}"
            .formatted(correlationId != null ? correlationId : "unknown", Instant.now().toString());
    byte[] bytes = Objects.requireNonNull(body.getBytes(StandardCharsets.UTF_8));
    DataBuffer dataBuffer =
        Objects.requireNonNull(response.bufferFactory(), "bufferFactory").wrap(bytes);
    return response.writeWith(Mono.just(Objects.requireNonNull(dataBuffer)));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }
}
