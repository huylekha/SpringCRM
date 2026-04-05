package com.company.platform.shared.security;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component("identityRequestContextFilter")
@Order(-200)
public class RequestContextFilter extends OncePerRequestFilter {

  public static final String HEADER_USER_ID = "X-User-Id";
  public static final String HEADER_USER_FULL_NAME = "X-User-Full-Name";
  public static final String HEADER_TENANT_ID = "X-Tenant-Id";

  private final ObjectProvider<EntityManager> entityManagerProvider;
  private final boolean strictMode;

  public RequestContextFilter(
      ObjectProvider<EntityManager> entityManagerProvider,
      @Value("${app.security.strict-identity-headers:false}") boolean strictMode) {
    this.entityManagerProvider = entityManagerProvider;
    this.strictMode = strictMode;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      UserContext ctx = buildUserContext(request);
      RequestContext.set(ctx);

      if (!ctx.isSystem()) {
        enableTenantFilter(ctx.tenantId());
      }

      log.debug("RequestContext set: userId={}, tenantId={}", ctx.userId(), ctx.tenantId());

      filterChain.doFilter(request, response);
    } finally {
      RequestContext.clear();
    }
  }

  private UserContext buildUserContext(HttpServletRequest request) {
    String userIdHeader = request.getHeader(HEADER_USER_ID);
    String fullNameHeader = request.getHeader(HEADER_USER_FULL_NAME);
    String tenantIdHeader = request.getHeader(HEADER_TENANT_ID);

    if (userIdHeader == null || userIdHeader.isBlank()) {
      if (strictMode) {
        log.warn(
            "Missing {} header in strict mode for path: {}",
            HEADER_USER_ID,
            request.getRequestURI());
      }
      return UserContext.SYSTEM;
    }

    try {
      UUID userId = UUID.fromString(userIdHeader.trim());
      String fullName =
          (fullNameHeader != null && !fullNameHeader.isBlank()) ? fullNameHeader.trim() : "UNKNOWN";
      String tenantId =
          (tenantIdHeader != null && !tenantIdHeader.isBlank()) ? tenantIdHeader.trim() : "DEFAULT";

      return new UserContext(userId, fullName, tenantId);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid UUID in {} header: {}", HEADER_USER_ID, userIdHeader);
      return UserContext.SYSTEM;
    }
  }

  private void enableTenantFilter(String tenantId) {
    EntityManager entityManager = entityManagerProvider.getIfAvailable();
    if (entityManager == null) {
      log.debug("EntityManager not available, skipping tenant filter enablement");
      return;
    }
    try {
      Session session = entityManager.unwrap(Session.class);
      session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    } catch (Exception e) {
      log.debug(
          "Could not enable tenant filter (expected during non-JPA requests): {}", e.getMessage());
    }
  }
}
