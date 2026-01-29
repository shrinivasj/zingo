package com.zingo.app.controller;

import com.zingo.app.dto.SafetyDtos.BlockRequest;
import com.zingo.app.dto.SafetyDtos.ReportRequest;
import com.zingo.app.entity.Profile;
import com.zingo.app.security.SecurityUtil;
import com.zingo.app.service.SafetyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SafetyController {
  private final SafetyService safetyService;

  public SafetyController(SafetyService safetyService) {
    this.safetyService = safetyService;
  }

  @PostMapping("/block")
  public void block(@Valid @RequestBody BlockRequest request) {
    safetyService.block(request);
  }

  @PostMapping("/report")
  public void report(@Valid @RequestBody ReportRequest request) {
    safetyService.report(request);
  }

  @GetMapping("/blocks")
  public List<Profile> blockedUsers() {
    Long userId = SecurityUtil.currentUserId();
    return safetyService.listBlockedProfiles(userId);
  }
}
