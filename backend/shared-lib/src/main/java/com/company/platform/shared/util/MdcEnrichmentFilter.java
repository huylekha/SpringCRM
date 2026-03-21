package com.company.platform.shared.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that enriches MDC with additional context information after authentication. This filter
 * runs after security filters to capture authenticated user information.
 */
@Slf4j
@Component
@Order(-100) // Run after security filters but before most other filters
public class MdcEnrichmentFilter extends OncePerRequestFilter {

  public static final String USER_ID_MDC_KEY = "userId";
  public static final String REQUEST_METHOD_MDC_KEY = "requestMethod";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      // Add request method to MDC
      MDC.put(REQUEST_METHOD_MDC_KEY, request.getMethod());

      // Add user ID to MDC if authenticated
      String userId = getCurrentUserId();
      if (userId != null) {
        MDC.put(USER_ID_MDC_KEY, userId);
      }

      filterChain.doFilter(request, response);

    } finally {
      // Clean up MDC to prevent memory leaks
      MDC.remove(USER_ID_MDC_KEY);
      MDC.remove(REQUEST_METHOD_MDC_KEY);
    }
  }

  /** Get current user ID from SecurityContext for logging. Returns null if not authenticated. */
  private String getCurrentUserId() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null
          && authentication.isAuthenticated()
          && !"anonymousUser".equals(authentication.getPrincipal())) {
        return authentication.getName();
      }
    } catch (Exception e) {
      // Ignore - SecurityContext not available or not populated yet
      log.debug("Could not retrieve user ID from SecurityContext: {}", e.getMessage());
    }
    return null;
  }
}
