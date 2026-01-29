package com.zingo.app.service;

import com.zingo.app.dto.LobbyDtos.LobbyPresenceUpdate;
import com.zingo.app.dto.LobbyDtos.LobbyUserDto;
import com.zingo.app.dto.LobbyDtos.LobbyUsersResponse;
import com.zingo.app.entity.LobbyPresence;
import com.zingo.app.entity.Profile;
import com.zingo.app.repository.LobbyPresenceRepository;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.security.SecurityUtil;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LobbyService {
  private final LobbyPresenceRepository lobbyPresenceRepository;
  private final ProfileRepository profileRepository;
  private final SafetyService safetyService;
  private final SimpMessagingTemplate messagingTemplate;
  private final long presenceTtlMinutes;

  public LobbyService(LobbyPresenceRepository lobbyPresenceRepository, ProfileRepository profileRepository,
      SafetyService safetyService, SimpMessagingTemplate messagingTemplate,
      @Value("${app.lobby.presenceTtlMinutes}") long presenceTtlMinutes) {
    this.lobbyPresenceRepository = lobbyPresenceRepository;
    this.profileRepository = profileRepository;
    this.safetyService = safetyService;
    this.messagingTemplate = messagingTemplate;
    this.presenceTtlMinutes = presenceTtlMinutes;
  }

  @Transactional
  public LobbyPresenceUpdate join(Long showtimeId) {
    Long userId = SecurityUtil.currentUserId();
    LobbyPresence presence = lobbyPresenceRepository.findByShowtimeIdAndUserId(showtimeId, userId)
        .orElseGet(LobbyPresence::new);
    presence.setShowtimeId(showtimeId);
    presence.setUserId(userId);
    presence.setLastSeenAt(Instant.now());
    lobbyPresenceRepository.save(presence);

    long count = lobbyPresenceRepository.countByShowtimeId(showtimeId);
    LobbyPresenceUpdate update = new LobbyPresenceUpdate(showtimeId, count, Instant.now());
    messagingTemplate.convertAndSend("/topic/lobby." + showtimeId, update);
    return update;
  }

  @Transactional
  public LobbyPresenceUpdate heartbeat(Long showtimeId) {
    return join(showtimeId);
  }

  public LobbyUsersResponse listUsers(Long showtimeId, int page, int size) {
    Long userId = SecurityUtil.currentUserId();
    Page<LobbyPresence> presencePage = lobbyPresenceRepository.findByShowtimeId(showtimeId, PageRequest.of(page, size));
    Set<Long> blockedIds = safetyService.blockedIdsForUser(userId);

    List<LobbyUserDto> users = new ArrayList<>();
    for (LobbyPresence presence : presencePage.getContent()) {
      Long otherUserId = presence.getUserId();
      if (otherUserId.equals(userId)) {
        continue;
      }
      if (blockedIds.contains(otherUserId)) {
        continue;
      }
      Profile profile = profileRepository.findById(otherUserId).orElse(null);
      if (profile == null) {
        continue;
      }
      users.add(new LobbyUserDto(
          profile.getUserId(),
          profile.getDisplayName(),
          profile.getAvatarUrl(),
          profile.getBioShort(),
          profile.getPersonalityTags()));
    }
    long total = lobbyPresenceRepository.countByShowtimeId(showtimeId);
    return new LobbyUsersResponse(showtimeId, total, users);
  }

  @Scheduled(fixedDelay = 60000)
  @Transactional
  public void cleanupStale() {
    Instant cutoff = Instant.now().minus(presenceTtlMinutes, ChronoUnit.MINUTES);
    lobbyPresenceRepository.deleteByLastSeenAtBefore(cutoff);
  }
}
