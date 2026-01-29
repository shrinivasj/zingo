package com.zingo.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {
  private SecurityUtil() {}

  public static Long currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      return null;
    }
    if (auth.getPrincipal() instanceof Long) {
      return (Long) auth.getPrincipal();
    }
    if (auth.getPrincipal() instanceof String) {
      return Long.valueOf((String) auth.getPrincipal());
    }
    return null;
  }
}
