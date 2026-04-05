package com.company.platform.shared.security;

import java.util.Optional;

public final class RequestContext {

  private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

  private RequestContext() {}

  public static void set(UserContext ctx) {
    HOLDER.set(ctx);
  }

  public static Optional<UserContext> get() {
    return Optional.ofNullable(HOLDER.get());
  }

  public static UserContext getRequired() {
    UserContext ctx = HOLDER.get();
    if (ctx == null) {
      throw new IllegalStateException(
          "RequestContext is not set. Ensure the request filter is active.");
    }
    return ctx;
  }

  public static UserContext current() {
    UserContext ctx = HOLDER.get();
    return ctx != null ? ctx : UserContext.SYSTEM;
  }

  public static void clear() {
    HOLDER.remove();
  }
}
