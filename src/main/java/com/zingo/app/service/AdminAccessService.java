package com.zingo.app.service;

import com.zingo.app.exception.ForbiddenException;
import com.zingo.app.security.JwtService;
import com.zingo.app.security.SecurityUtil;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAccessService {
  private final Set<Long> ownerUserIds;

  public AdminAccessService(@Value("${app.admin.ownerUserIds:}") String ownerUserIds) {
    this.ownerUserIds = Arrays.stream(ownerUserIds.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .map(Long::valueOf)
        .collect(Collectors.toSet());
  }

  public boolean isCurrentUserOwner() {
    JwtService.JwtUser jwtUser = SecurityUtil.currentJwtUser();
    if (jwtUser == null || jwtUser.userId() == null) {
      return false;
    }
    return ownerUserIds.contains(jwtUser.userId());
  }

  public void assertCurrentUserIsOwner() {
    if (!isCurrentUserOwner()) {
      throw new ForbiddenException("Owner access required");
    }
  }
}
