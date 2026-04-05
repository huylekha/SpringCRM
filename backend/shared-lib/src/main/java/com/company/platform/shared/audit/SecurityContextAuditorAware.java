package com.company.platform.shared.audit;

import com.company.platform.shared.security.RequestContext;
import com.company.platform.shared.security.UserContext;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component("auditorAware")
@Slf4j
public class SecurityContextAuditorAware implements AuditorAware<String> {

  @Override
  public Optional<String> getCurrentAuditor() {
    UserContext ctx = RequestContext.current();
    String auditor = ctx.userId().toString();
    log.debug("Current auditor: {}", auditor);
    return Optional.of(auditor);
  }

  public String getCurrentAuditorSync() {
    return getCurrentAuditor().orElse(UserContext.SYSTEM.userId().toString());
  }

  public boolean hasAuthenticatedUser() {
    return RequestContext.get().map(ctx -> !ctx.isSystem()).orElse(false);
  }
}
