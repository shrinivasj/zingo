package com.zingo.app.controller;

import com.zingo.app.dto.WaitlistDtos.JoinWaitlistRequest;
import com.zingo.app.dto.WaitlistDtos.JoinWaitlistResponse;
import com.zingo.app.service.WaitlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/waitlist")
public class WaitlistController {
  private final WaitlistService waitlistService;

  public WaitlistController(WaitlistService waitlistService) {
    this.waitlistService = waitlistService;
  }

  @PostMapping
  public JoinWaitlistResponse join(@Valid @RequestBody JoinWaitlistRequest request, HttpServletRequest http) {
    String userAgent = http.getHeader("User-Agent");
    String ip = clientIp(http);
    return waitlistService.join(request, ip, userAgent);
  }

  private static String clientIp(HttpServletRequest req) {
    String forwarded = req.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      String first = forwarded.split(",")[0].trim();
      if (!first.isBlank()) return first;
    }
    String realIp = req.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) return realIp.trim();
    return req.getRemoteAddr();
  }
}

