package com.zingo.app.service;

import com.zingo.app.dto.NotificationDtos.NotificationDto;
import com.zingo.app.entity.Event;
import com.zingo.app.entity.Notification;
import com.zingo.app.entity.NotificationType;
import com.zingo.app.entity.Profile;
import com.zingo.app.entity.Showtime;
import com.zingo.app.repository.EventRepository;
import com.zingo.app.repository.NotificationRepository;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.repository.ShowtimeRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final ShowtimeRepository showtimeRepository;
  private final EventRepository eventRepository;
  private final ProfileRepository profileRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final PushDeliveryService pushDeliveryService;

  public NotificationService(
      NotificationRepository notificationRepository,
      ShowtimeRepository showtimeRepository,
      EventRepository eventRepository,
      ProfileRepository profileRepository,
      SimpMessagingTemplate messagingTemplate,
      PushDeliveryService pushDeliveryService) {
    this.notificationRepository = notificationRepository;
    this.showtimeRepository = showtimeRepository;
    this.eventRepository = eventRepository;
    this.profileRepository = profileRepository;
    this.messagingTemplate = messagingTemplate;
    this.pushDeliveryService = pushDeliveryService;
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
    pushDeliveryService.sendToUser(userId, buildTitle(type, dto.payload()), buildBody(type, dto.payload()),
        buildData(type, dto.payload(), dto.id()));
    return dto;
  }

  public List<NotificationDto> listForUser(Long userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public NotificationDto markRead(Long userId, Long id) {
    Notification notification = notificationRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new com.zingo.app.exception.NotFoundException("Notification not found"));
    if (notification.getReadAt() == null) {
      notification.setReadAt(Instant.now());
    }
    return toDto(notificationRepository.save(notification));
  }

  public NotificationDto toDto(Notification notification) {
    Map<String, Object> payload = enrichPayload(notification.getType(), notification.getPayloadJson());
    return new NotificationDto(
        notification.getId(),
        notification.getType(),
        payload,
        notification.getReadAt(),
        notification.getCreatedAt());
  }

  private Map<String, Object> enrichPayload(NotificationType type, Map<String, Object> payload) {
    if (payload == null) {
      return null;
    }
    if (type != NotificationType.INVITE) {
      return payload;
    }

    Map<String, Object> enriched = new LinkedHashMap<>(payload);

    Long showtimeId = asLong(enriched.get("showtimeId"));
    if (showtimeId != null) {
      Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
      if (showtime != null) {
        if (!enriched.containsKey("startsAt") || enriched.get("startsAt") == null) {
          enriched.put("startsAt", showtime.getStartsAt() != null ? showtime.getStartsAt().toString() : null);
        }
        if (!enriched.containsKey("eventTitle") || enriched.get("eventTitle") == null) {
          Event event = eventRepository.findById(showtime.getEventId()).orElse(null);
          if (event != null) {
            enriched.put("eventTitle", event.getTitle());
          }
        }
      }
    }

    Long fromUserId = asLong(enriched.get("fromUserId"));
    if (fromUserId != null) {
      Profile fromProfile = profileRepository.findById(fromUserId).orElse(null);
      if (fromProfile != null) {
        if (!enriched.containsKey("fromDisplayName") || enriched.get("fromDisplayName") == null) {
          enriched.put("fromDisplayName", fromProfile.getDisplayName());
        }
        if (!enriched.containsKey("fromAvatarUrl") || enriched.get("fromAvatarUrl") == null) {
          enriched.put("fromAvatarUrl", fromProfile.getAvatarUrl());
        }
      }
    }
    return enriched;
  }

  private Long asLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text) {
      try {
        return Long.parseLong(text);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private String buildTitle(NotificationType type, Map<String, Object> payload) {
    if (type == NotificationType.INVITE) {
      return "New Invite";
    }
    return "Aurofly";
  }

  private String buildBody(NotificationType type, Map<String, Object> payload) {
    if (payload == null) {
      return "You have a new update";
    }
    String payloadType = String.valueOf(payload.get("type"));
    if ("TREK_JOIN_REQUEST".equals(payloadType)) {
      String from = String.valueOf(payload.getOrDefault("fromDisplayName", "Someone"));
      return from + " requested to join your trek";
    }
    if ("TREK_JOIN_APPROVED".equals(payloadType)) {
      return "Your trek request was approved";
    }
    if ("TREK_JOIN_DECLINED".equals(payloadType)) {
      return "Your trek request was declined";
    }
    if (type == NotificationType.INVITE) {
      String from = String.valueOf(payload.getOrDefault("fromDisplayName", "Someone"));
      String event = String.valueOf(payload.getOrDefault("eventTitle", "a show"));
      return from + " invited you for " + event;
    }
    if ("INVITE_ACCEPTED".equals(String.valueOf(payload.get("type")))) {
      String from = String.valueOf(payload.getOrDefault("fromDisplayName", "Someone"));
      return from + " accepted your invite";
    }
    return "You have a new update";
  }

  private Map<String, Object> buildData(NotificationType type, Map<String, Object> payload, Long notificationId) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("pushType", type != null ? type.name() : "SYSTEM");
    data.put("notificationId", notificationId);
    if (payload != null) {
      if (payload.get("inviteId") != null) {
        data.put("inviteId", payload.get("inviteId"));
      }
      if (payload.get("showtimeId") != null) {
        data.put("showtimeId", payload.get("showtimeId"));
      }
      if (payload.get("conversationId") != null) {
        data.put("conversationId", payload.get("conversationId"));
      }
    }
    return data;
  }
}
