package com.zingo.app.controller;

import com.zingo.app.dto.NotificationDtos.NotificationDto;
import com.zingo.app.security.SecurityUtil;
import com.zingo.app.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public List<NotificationDto> list() {
    Long userId = SecurityUtil.currentUserId();
    return notificationService.listForUser(userId);
  }

  @PostMapping("/{id}/read")
  public NotificationDto markRead(@PathVariable Long id) {
    Long userId = SecurityUtil.currentUserId();
    return notificationService.markRead(userId, id);
  }
}
