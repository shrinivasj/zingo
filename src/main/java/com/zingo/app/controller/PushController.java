package com.zingo.app.controller;

import com.zingo.app.dto.PushDtos.RegisterPushTokenRequest;
import com.zingo.app.dto.PushDtos.UnregisterPushTokenRequest;
import com.zingo.app.security.SecurityUtil;
import com.zingo.app.service.PushTokenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
public class PushController {
  private final PushTokenService pushTokenService;

  public PushController(PushTokenService pushTokenService) {
    this.pushTokenService = pushTokenService;
  }

  @PostMapping("/register")
  public void register(@Valid @RequestBody RegisterPushTokenRequest request) {
    Long userId = SecurityUtil.currentUserId();
    pushTokenService.registerToken(userId, request.token(), request.platform());
  }

  @PostMapping("/unregister")
  public void unregister(@Valid @RequestBody UnregisterPushTokenRequest request) {
    Long userId = SecurityUtil.currentUserId();
    pushTokenService.unregisterToken(userId, request.token());
  }
}
