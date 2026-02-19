package com.zingo.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {
  private SecurityUtil() {}

  public static Long currentUserId() {
    JwtService.JwtUser jwtUser = currentJwtUser();
    if (jwtUser != null && jwtUser.userId() != null) {
      return jwtUser.userId();
    }

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

  public static JwtService.JwtUser currentJwtUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      return null;
    }
    if (auth.getPrincipal() instanceof JwtService.JwtUser jwtUser) {
      return jwtUser;
    }
    return null;
  }
}
