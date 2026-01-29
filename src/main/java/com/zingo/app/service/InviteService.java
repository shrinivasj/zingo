package com.zingo.app.service;

import com.zingo.app.dto.InviteDtos.InviteAcceptResponse;
import com.zingo.app.dto.InviteDtos.InviteDto;
import com.zingo.app.dto.InviteDtos.InviteRequest;
import com.zingo.app.entity.Invite;
import com.zingo.app.entity.InviteStatus;
import com.zingo.app.entity.NotificationType;
import com.zingo.app.entity.Profile;
import com.zingo.app.entity.Showtime;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.exception.NotFoundException;
import com.zingo.app.exception.TooManyRequestsException;
import com.zingo.app.repository.InviteRepository;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.repository.ShowtimeRepository;
import com.zingo.app.security.SecurityUtil;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InviteService {
  private final InviteRepository inviteRepository;
  private final ShowtimeRepository showtimeRepository;
  private final ProfileRepository profileRepository;
  private final SafetyService safetyService;
  private final NotificationService notificationService;
  private final ConversationService conversationService;
  private final int dailyLimit;
  private final int cooldownSeconds;

  public InviteService(InviteRepository inviteRepository, ShowtimeRepository showtimeRepository,
      ProfileRepository profileRepository, SafetyService safetyService, NotificationService notificationService,
      ConversationService conversationService,
      @Value("${app.invites.dailyLimit}") int dailyLimit,
      @Value("${app.invites.cooldownSeconds}") int cooldownSeconds) {
    this.inviteRepository = inviteRepository;
    this.showtimeRepository = showtimeRepository;
    this.profileRepository = profileRepository;
    this.safetyService = safetyService;
    this.notificationService = notificationService;
    this.conversationService = conversationService;
    this.dailyLimit = dailyLimit;
    this.cooldownSeconds = cooldownSeconds;
  }

  @Transactional
  public InviteDto createInvite(InviteRequest request) {
    Long fromUserId = SecurityUtil.currentUserId();
    if (fromUserId.equals(request.toUserId())) {
      throw new BadRequestException("Cannot invite yourself");
    }
    if (safetyService.isBlockedBetween(fromUserId, request.toUserId())) {
      throw new BadRequestException("Cannot invite this user");
    }
    profileRepository.findById(request.toUserId())
        .orElseThrow(() -> new NotFoundException("User not found"));
    Showtime showtime = showtimeRepository.findById(request.showtimeId())
        .orElseThrow(() -> new NotFoundException("Showtime not found"));

    Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
    long invitesToday = inviteRepository.countByFromUserIdAndCreatedAtAfter(fromUserId, startOfDay);
    if (invitesToday >= dailyLimit) {
      throw new TooManyRequestsException("Daily invite limit reached");
    }

    inviteRepository.findTopByFromUserIdOrderByCreatedAtDesc(fromUserId).ifPresent(invite -> {
      if (invite.getCreatedAt().isAfter(Instant.now().minusSeconds(cooldownSeconds))) {
        throw new TooManyRequestsException("Please wait before sending another invite");
      }
    });

    Invite invite = new Invite();
    invite.setFromUserId(fromUserId);
    invite.setToUserId(request.toUserId());
    invite.setShowtimeId(showtime.getId());
    invite.setStatus(InviteStatus.PENDING);
    invite = inviteRepository.save(invite);

    Profile fromProfile = profileRepository.findById(fromUserId).orElse(null);
    Map<String, Object> payload = new HashMap<>();
    payload.put("inviteId", invite.getId());
    payload.put("fromUserId", fromUserId);
    payload.put("fromDisplayName", fromProfile != null ? fromProfile.getDisplayName() : "Someone");
    payload.put("showtimeId", showtime.getId());
    payload.put("startsAt", showtime.getStartsAt());
    notificationService.createAndSend(request.toUserId(), NotificationType.INVITE, payload);

    return toDto(invite);
  }

  @Transactional
  public InviteAcceptResponse acceptInvite(Long inviteId) {
    Long userId = SecurityUtil.currentUserId();
    Invite invite = inviteRepository.findByIdAndToUserId(inviteId, userId)
        .orElseThrow(() -> new NotFoundException("Invite not found"));
    if (invite.getStatus() != InviteStatus.PENDING) {
      throw new BadRequestException("Invite already handled");
    }
    invite.setStatus(InviteStatus.ACCEPTED);
    Invite saved = inviteRepository.save(invite);
    Long conversationId = conversationService.openConversation(invite.getShowtimeId(), invite.getFromUserId(), userId);
    return new InviteAcceptResponse(toDto(saved), conversationId);
  }

  @Transactional
  public InviteDto declineInvite(Long inviteId) {
    Long userId = SecurityUtil.currentUserId();
    Invite invite = inviteRepository.findByIdAndToUserId(inviteId, userId)
        .orElseThrow(() -> new NotFoundException("Invite not found"));
    if (invite.getStatus() != InviteStatus.PENDING) {
      throw new BadRequestException("Invite already handled");
    }
    invite.setStatus(InviteStatus.DECLINED);
    return toDto(inviteRepository.save(invite));
  }

  public InviteDto toDto(Invite invite) {
    return new InviteDto(
        invite.getId(),
        invite.getFromUserId(),
        invite.getToUserId(),
        invite.getShowtimeId(),
        invite.getStatus(),
        invite.getCreatedAt(),
        invite.getUpdatedAt());
  }
}
