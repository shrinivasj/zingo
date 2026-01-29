package com.zingo.app.service;

import com.zingo.app.dto.NotificationDtos.NotificationDto;
import com.zingo.app.entity.Notification;
import com.zingo.app.entity.NotificationType;
import com.zingo.app.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final SimpMessagingTemplate messagingTemplate;

  public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
    this.notificationRepository = notificationRepository;
    this.messagingTemplate = messagingTemplate;
  }

  @Transactional
  public NotificationDto createAndSend(Long userId, NotificationType type, Map<String, Object> payload) {
    Notification notification = new Notification();
    notification.setUserId(userId);
    notification.setType(type);
    notification.setPayloadJson(payload);
    notification = notificationRepository.save(notification);

    NotificationDto dto = toDto(notification);
    messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/notifications", dto);
    return dto;
  }

  public List<NotificationDto> listForUser(Long userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public NotificationDto markRead(Long userId, Long id) {
    Notification notification = notificationRepository.findById(id)
        .orElseThrow(() -> new com.zingo.app.exception.NotFoundException("Notification not found"));
    if (!notification.getUserId().equals(userId)) {
      return toDto(notification);
    }
    if (notification.getReadAt() == null) {
      notification.setReadAt(Instant.now());
    }
    return toDto(notificationRepository.save(notification));
  }

  public NotificationDto toDto(Notification notification) {
    return new NotificationDto(
        notification.getId(),
        notification.getType(),
        notification.getPayloadJson(),
        notification.getReadAt(),
        notification.getCreatedAt());
  }
}
