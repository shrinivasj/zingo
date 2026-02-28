package com.zingo.app.service;

import com.zingo.app.dto.TrekDtos.CreateTrekGroupRequest;
import com.zingo.app.dto.TrekDtos.CreateTrekJoinRequest;
import com.zingo.app.dto.TrekDtos.TrekDecisionResponse;
import com.zingo.app.dto.TrekDtos.TrekGroupDto;
import com.zingo.app.dto.TrekDtos.TrekHostStatusDto;
import com.zingo.app.dto.TrekDtos.TrekJoinRequestDto;
import com.zingo.app.entity.Event;
import com.zingo.app.entity.EventType;
import com.zingo.app.entity.NotificationType;
import com.zingo.app.entity.Profile;
import com.zingo.app.entity.Showtime;
import com.zingo.app.entity.TrekGroup;
import com.zingo.app.entity.TrekJoinRequest;
import com.zingo.app.entity.TrekJoinRequestStatus;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.exception.ForbiddenException;
import com.zingo.app.exception.NotFoundException;
import com.zingo.app.repository.EventRepository;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.repository.ShowtimeRepository;
import com.zingo.app.repository.TrekGroupRepository;
import com.zingo.app.repository.TrekJoinRequestRepository;
import com.zingo.app.security.SecurityUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrekService {
  private final TrekGroupRepository trekGroupRepository;
  private final TrekJoinRequestRepository trekJoinRequestRepository;
  private final ShowtimeRepository showtimeRepository;
  private final EventRepository eventRepository;
  private final ProfileRepository profileRepository;
  private final NotificationService notificationService;
  private final SafetyService safetyService;
  private final ConversationService conversationService;

  public TrekService(
      TrekGroupRepository trekGroupRepository,
      TrekJoinRequestRepository trekJoinRequestRepository,
      ShowtimeRepository showtimeRepository,
      EventRepository eventRepository,
      ProfileRepository profileRepository,
      NotificationService notificationService,
      SafetyService safetyService,
      ConversationService conversationService) {
    this.trekGroupRepository = trekGroupRepository;
    this.trekJoinRequestRepository = trekJoinRequestRepository;
    this.showtimeRepository = showtimeRepository;
    this.eventRepository = eventRepository;
    this.profileRepository = profileRepository;
    this.notificationService = notificationService;
    this.safetyService = safetyService;
    this.conversationService = conversationService;
  }

  @Transactional
  public TrekGroupDto createGroup(CreateTrekGroupRequest request) {
    Long hostUserId = SecurityUtil.currentUserId();
    Profile hostProfile = profileRepository.findById(hostUserId).orElseThrow(() -> new NotFoundException("Profile not found"));
    if (!hostProfile.isTrekHostEnabled()) {
      throw new BadRequestException("Complete trek host onboarding first");
    }
    Showtime showtime = requireTrekShowtime(request.showtimeId());
    if (trekGroupRepository.existsByShowtimeId(showtime.getId())) {
      throw new BadRequestException("Trek group already exists for this trek. You can request to join.");
    }

    TrekGroup group = new TrekGroup();
    group.setShowtimeId(showtime.getId());
    group.setHostUserId(hostUserId);
    group.setDescription(blankToNull(request.description()));
    group.setMaxMembers(request.maxMembers());
    Long groupConversationId = conversationService.createGroupConversation(showtime.getId(), hostUserId);
    group.setConversationId(groupConversationId);
    TrekGroup saved = trekGroupRepository.save(group);

    return toGroupDto(saved, hostProfile, 0);
  }

  public TrekHostStatusDto hostStatus() {
    Long userId = SecurityUtil.currentUserId();
    Profile profile = profileRepository.findById(userId).orElseThrow(() -> new NotFoundException("Profile not found"));
    return new TrekHostStatusDto(profile.isTrekHostEnabled());
  }

  @Transactional
  public TrekHostStatusDto onboardCurrentUserAsHost() {
    Long userId = SecurityUtil.currentUserId();
    Profile profile = profileRepository.findById(userId).orElseThrow(() -> new NotFoundException("Profile not found"));
    if (!profile.isTrekHostEnabled()) {
      profile.setTrekHostEnabled(true);
      profileRepository.save(profile);
    }
    return new TrekHostStatusDto(true);
  }

  public List<TrekGroupDto> listGroups(Long showtimeId) {
    requireTrekShowtime(showtimeId);
    List<TrekGroup> groups = trekGroupRepository.findByShowtimeIdOrderByCreatedAtAsc(showtimeId);
    return toGroupDtos(groups);
  }

  public List<TrekJoinRequestDto> listPendingForCurrentHost() {
    Long hostUserId = SecurityUtil.currentUserId();
    List<TrekGroup> groups = trekGroupRepository.findByHostUserIdOrderByCreatedAtDesc(hostUserId);
    if (groups.isEmpty()) {
      return List.of();
    }
    List<Long> groupIds = groups.stream().map(TrekGroup::getId).toList();
    Map<Long, TrekGroup> groupById = new HashMap<>();
    for (TrekGroup group : groups) {
      groupById.put(group.getId(), group);
    }
    List<TrekJoinRequest> requests = trekJoinRequestRepository.findByGroupIdInAndStatusOrderByCreatedAtDesc(
        groupIds,
        TrekJoinRequestStatus.PENDING);
    return toRequestDtos(requests, groupById);
  }

  @Transactional
  public TrekJoinRequestDto requestJoin(Long groupId, CreateTrekJoinRequest request) {
    Long requesterUserId = SecurityUtil.currentUserId();
    TrekGroup group = trekGroupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("Trek group not found"));
    requireTrekShowtime(group.getShowtimeId());

    if (requesterUserId.equals(group.getHostUserId())) {
      throw new BadRequestException("You cannot request to join your own trek group");
    }
    if (safetyService.isBlockedBetween(requesterUserId, group.getHostUserId())) {
      throw new BadRequestException("Cannot request this trek group");
    }
    if (trekJoinRequestRepository.existsByGroupIdAndRequesterUserIdAndStatus(
        groupId,
        requesterUserId,
        TrekJoinRequestStatus.PENDING)) {
      throw new BadRequestException("You already have a pending request for this trek group");
    }
    if (trekJoinRequestRepository.existsByGroupIdAndRequesterUserIdAndStatus(
        groupId,
        requesterUserId,
        TrekJoinRequestStatus.APPROVED)) {
      throw new BadRequestException("You are already approved in this trek group");
    }

    TrekJoinRequest joinRequest = new TrekJoinRequest();
    joinRequest.setGroupId(groupId);
    joinRequest.setRequesterUserId(requesterUserId);
    joinRequest.setStatus(TrekJoinRequestStatus.PENDING);
    joinRequest.setNote(blankToNull(request != null ? request.note() : null));
    TrekJoinRequest saved = trekJoinRequestRepository.save(joinRequest);

    Profile requester = profileRepository.findById(requesterUserId).orElse(null);
    Showtime showtime = showtimeRepository.findById(group.getShowtimeId()).orElse(null);
    Event event = showtime != null ? eventRepository.findById(showtime.getEventId()).orElse(null) : null;

    Map<String, Object> payload = new HashMap<>();
    payload.put("type", "TREK_JOIN_REQUEST");
    payload.put("requestStatus", "PENDING");
    payload.put("trekJoinRequestId", saved.getId());
    payload.put("groupId", group.getId());
    payload.put("showtimeId", group.getShowtimeId());
    payload.put("fromUserId", requesterUserId);
    payload.put("fromDisplayName", requester != null ? requester.getDisplayName() : "Someone");
    payload.put("fromAvatarUrl", requester != null ? requester.getAvatarUrl() : null);
    payload.put("eventTitle", event != null ? event.getTitle() : "Trek");
    payload.put("startsAt", showtime != null && showtime.getStartsAt() != null ? showtime.getStartsAt().toString() : null);
    payload.put("note", saved.getNote());
    notificationService.createAndSend(group.getHostUserId(), NotificationType.SYSTEM, payload);

    return toRequestDto(saved, group, requester);
  }

  @Transactional
  public TrekDecisionResponse approveRequest(Long requestId) {
    Long hostUserId = SecurityUtil.currentUserId();
    TrekJoinRequest joinRequest = trekJoinRequestRepository.findByIdAndStatus(requestId, TrekJoinRequestStatus.PENDING)
        .orElseThrow(() -> new NotFoundException("Join request not found"));
    TrekGroup group = trekGroupRepository.findById(joinRequest.getGroupId())
        .orElseThrow(() -> new NotFoundException("Trek group not found"));
    ensureHost(group, hostUserId);

    Long conversationId = group.getConversationId();
    if (conversationId == null) {
      conversationId = conversationService.createGroupConversation(group.getShowtimeId(), group.getHostUserId());
      group.setConversationId(conversationId);
      trekGroupRepository.save(group);
    }

    TrekJoinRequest existingApproved = trekJoinRequestRepository
        .findFirstByGroupIdAndRequesterUserIdAndStatusOrderByUpdatedAtDesc(
            group.getId(),
            joinRequest.getRequesterUserId(),
            TrekJoinRequestStatus.APPROVED)
        .orElse(null);
    if (existingApproved != null) {
      conversationService.addMemberToConversation(conversationId, joinRequest.getRequesterUserId());
      if (!existingApproved.getId().equals(joinRequest.getId())) {
        joinRequest.setStatus(TrekJoinRequestStatus.DECLINED);
        joinRequest.setReviewedAt(Instant.now());
        trekJoinRequestRepository.save(joinRequest);
      }
      Profile requester = profileRepository.findById(existingApproved.getRequesterUserId()).orElse(null);
      return new TrekDecisionResponse(toRequestDto(existingApproved, group, requester), conversationId);
    }

    conversationService.addMemberToConversation(conversationId, joinRequest.getRequesterUserId());

    joinRequest.setStatus(TrekJoinRequestStatus.APPROVED);
    joinRequest.setReviewedAt(Instant.now());
    TrekJoinRequest saved = trekJoinRequestRepository.save(joinRequest);

    Profile host = profileRepository.findById(hostUserId).orElse(null);
    Map<String, Object> payload = new HashMap<>();
    payload.put("type", "TREK_JOIN_APPROVED");
    payload.put("requestStatus", "APPROVED");
    payload.put("trekJoinRequestId", saved.getId());
    payload.put("groupId", group.getId());
    payload.put("showtimeId", group.getShowtimeId());
    payload.put("conversationId", conversationId);
    payload.put("fromUserId", hostUserId);
    payload.put("fromDisplayName", host != null ? host.getDisplayName() : "Trek host");
    payload.put("fromAvatarUrl", host != null ? host.getAvatarUrl() : null);
    notificationService.createAndSend(saved.getRequesterUserId(), NotificationType.SYSTEM, payload);

    Profile requester = profileRepository.findById(saved.getRequesterUserId()).orElse(null);
    return new TrekDecisionResponse(toRequestDto(saved, group, requester), conversationId);
  }

  @Transactional
  public TrekJoinRequestDto declineRequest(Long requestId) {
    Long hostUserId = SecurityUtil.currentUserId();
    TrekJoinRequest joinRequest = trekJoinRequestRepository.findByIdAndStatus(requestId, TrekJoinRequestStatus.PENDING)
        .orElseThrow(() -> new NotFoundException("Join request not found"));
    TrekGroup group = trekGroupRepository.findById(joinRequest.getGroupId())
        .orElseThrow(() -> new NotFoundException("Trek group not found"));
    ensureHost(group, hostUserId);

    joinRequest.setStatus(TrekJoinRequestStatus.DECLINED);
    joinRequest.setReviewedAt(Instant.now());
    TrekJoinRequest saved = trekJoinRequestRepository.save(joinRequest);

    Profile host = profileRepository.findById(hostUserId).orElse(null);
    Map<String, Object> payload = new HashMap<>();
    payload.put("type", "TREK_JOIN_DECLINED");
    payload.put("requestStatus", "DECLINED");
    payload.put("trekJoinRequestId", saved.getId());
    payload.put("groupId", group.getId());
    payload.put("showtimeId", group.getShowtimeId());
    payload.put("fromUserId", hostUserId);
    payload.put("fromDisplayName", host != null ? host.getDisplayName() : "Trek host");
    payload.put("fromAvatarUrl", host != null ? host.getAvatarUrl() : null);
    notificationService.createAndSend(saved.getRequesterUserId(), NotificationType.SYSTEM, payload);

    Profile requester = profileRepository.findById(saved.getRequesterUserId()).orElse(null);
    return toRequestDto(saved, group, requester);
  }

  private List<TrekGroupDto> toGroupDtos(List<TrekGroup> groups) {
    if (groups.isEmpty()) {
      return List.of();
    }

    Set<Long> hostIds = new HashSet<>();
    for (TrekGroup group : groups) {
      hostIds.add(group.getHostUserId());
    }
    Map<Long, Profile> profiles = new HashMap<>();
    for (Profile profile : profileRepository.findAllById(hostIds)) {
      profiles.put(profile.getUserId(), profile);
    }

    List<TrekGroupDto> dtos = new ArrayList<>(groups.size());
    for (TrekGroup group : groups) {
      Profile hostProfile = profiles.get(group.getHostUserId());
      long pending = trekJoinRequestRepository.countByGroupIdAndStatus(group.getId(), TrekJoinRequestStatus.PENDING);
      dtos.add(toGroupDto(group, hostProfile, pending));
    }
    return dtos;
  }

  private List<TrekJoinRequestDto> toRequestDtos(List<TrekJoinRequest> requests, Map<Long, TrekGroup> groupById) {
    if (requests.isEmpty()) {
      return List.of();
    }
    Set<Long> requesterIds = new HashSet<>();
    for (TrekJoinRequest request : requests) {
      requesterIds.add(request.getRequesterUserId());
    }
    Map<Long, Profile> profiles = new HashMap<>();
    for (Profile profile : profileRepository.findAllById(requesterIds)) {
      profiles.put(profile.getUserId(), profile);
    }

    List<TrekJoinRequestDto> dtos = new ArrayList<>(requests.size());
    for (TrekJoinRequest request : requests) {
      TrekGroup group = groupById.get(request.getGroupId());
      if (group == null) {
        continue;
      }
      dtos.add(toRequestDto(request, group, profiles.get(request.getRequesterUserId())));
    }
    return dtos;
  }

  private TrekGroupDto toGroupDto(TrekGroup group, Profile hostProfile, long pending) {
    return new TrekGroupDto(
        group.getId(),
        group.getShowtimeId(),
        group.getHostUserId(),
        hostProfile != null ? hostProfile.getDisplayName() : "Trek host",
        hostProfile != null ? hostProfile.getAvatarUrl() : null,
        group.getDescription(),
        group.getMaxMembers(),
        pending,
        group.getCreatedAt());
  }

  private TrekJoinRequestDto toRequestDto(TrekJoinRequest request, TrekGroup group, Profile requesterProfile) {
    return new TrekJoinRequestDto(
        request.getId(),
        request.getGroupId(),
        group.getShowtimeId(),
        request.getRequesterUserId(),
        requesterProfile != null ? requesterProfile.getDisplayName() : "User",
        requesterProfile != null ? requesterProfile.getAvatarUrl() : null,
        request.getNote(),
        request.getStatus(),
        request.getCreatedAt(),
        request.getUpdatedAt(),
        request.getReviewedAt());
  }

  private Showtime requireTrekShowtime(Long showtimeId) {
    Showtime showtime = showtimeRepository.findById(showtimeId).orElseThrow(() -> new NotFoundException("Showtime not found"));
    Event event = eventRepository.findById(showtime.getEventId()).orElseThrow(() -> new NotFoundException("Event not found"));
    if (event.getType() != EventType.TREK) {
      throw new BadRequestException("Trek groups are only available for trek plans");
    }
    return showtime;
  }

  private void ensureHost(TrekGroup group, Long currentUserId) {
    if (!group.getHostUserId().equals(currentUserId)) {
      throw new ForbiddenException("Only trek host can perform this action");
    }
  }

  private String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
