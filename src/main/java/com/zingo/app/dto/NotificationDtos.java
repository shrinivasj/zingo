package com.zingo.app.dto;

import com.zingo.app.entity.NotificationType;
import java.time.Instant;
import java.util.Map;

public class NotificationDtos {
  public record NotificationDto(
      Long id,
      NotificationType type,
      Map<String, Object> payload,
      Instant readAt,
      Instant createdAt) {}
}
