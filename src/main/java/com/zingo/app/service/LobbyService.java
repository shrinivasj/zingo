package com.zingo.app.service;

import com.zingo.app.dto.LobbyDtos.LobbyPresenceUpdate;
import com.zingo.app.dto.LobbyDtos.LobbyUserDto;
import com.zingo.app.dto.LobbyDtos.LobbyUsersResponse;
import com.zingo.app.dto.LobbyDtos.ActiveLobbyDto;
import com.zingo.app.entity.Event;
import com.zingo.app.entity.EventType;
import com.zingo.app.entity.LobbyPresence;
import com.zingo.app.entity.Profile;
import com.zingo.app.entity.Showtime;
import com.zingo.app.entity.Venue;
import com.zingo.app.repository.EventRepository;
import com.zingo.app.repository.LobbyPresenceRepository;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.repository.ShowtimeRepository;
import com.zingo.app.repository.VenueRepository;
import com.zingo.app.security.SecurityUtil;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private final ShowtimeRepository showtimeRepository;
  private final EventRepository eventRepository;
  private final VenueRepository venueRepository;
  private final SafetyService safetyService;
  private final SimpMessagingTemplate messagingTemplate;
  private final long presenceTtlMinutes;

  public LobbyService(LobbyPresenceRepository lobbyPresenceRepository, ProfileRepository profileRepository,
      ShowtimeRepository showtimeRepository, EventRepository eventRepository, VenueRepository venueRepository,
      SafetyService safetyService, SimpMessagingTemplate messagingTemplate,
      @Value("${app.lobby.presenceTtlMinutes}") long presenceTtlMinutes) {
    this.lobbyPresenceRepository = lobbyPresenceRepository;
    this.profileRepository = profileRepository;
    this.showtimeRepository = showtimeRepository;
    this.eventRepository = eventRepository;
    this.venueRepository = venueRepository;
    this.safetyService = safetyService;
    this.messagingTemplate = messagingTemplate;
    this.presenceTtlMinutes = presenceTtlMinutes;
  }

  @Transactional
  public LobbyPresenceUpdate join(Long showtimeId) {
    Long userId = SecurityUtil.currentUserId();
    lobbyPresenceRepository.upsertPresence(showtimeId, userId, Instant.now());

    long count = lobbyPresenceRepository.countByShowtimeId(showtimeId);
    LobbyPresenceUpdate update = new LobbyPresenceUpdate(showtimeId, count, Instant.now());
    messagingTemplate.convertAndSend("/topic/lobby." + showtimeId, update);
    return update;
  }

  @Transactional
  public LobbyPresenceUpdate heartbeat(Long showtimeId) {
    return join(showtimeId);
  }

  @Transactional
  public LobbyPresenceUpdate leave(Long showtimeId) {
    Long userId = SecurityUtil.currentUserId();
    lobbyPresenceRepository.deleteByShowtimeIdAndUserId(showtimeId, userId);
    long count = lobbyPresenceRepository.countByShowtimeId(showtimeId);
    LobbyPresenceUpdate update = new LobbyPresenceUpdate(showtimeId, count, Instant.now());
    messagingTemplate.convertAndSend("/topic/lobby." + showtimeId, update);
    return update;
  }

  public LobbyUsersResponse listUsers(Long showtimeId, int page, int size) {
    Long userId = SecurityUtil.currentUserId();
    Page<LobbyPresence> presencePage = lobbyPresenceRepository.findByShowtimeId(showtimeId, PageRequest.of(page, size));
    Set<Long> blockedIds = safetyService.blockedIdsForUser(userId);

    String eventType = null;
    Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
    if (showtime != null) {
      Event event = eventRepository.findById(showtime.getEventId()).orElse(null);
      if (event != null && event.getType() != null) {
        eventType = event.getType().name();
      }
    }

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
    return new LobbyUsersResponse(showtimeId, total, users, eventType);
  }

  public List<ActiveLobbyDto> listActiveForCurrentUser() {
    Long userId = SecurityUtil.currentUserId();
    List<LobbyPresence> presences = lobbyPresenceRepository.findByUserId(userId);
    if (presences.isEmpty()) {
      return List.of();
    }

    List<Long> showtimeIds = presences.stream().map(LobbyPresence::getShowtimeId).distinct().toList();
    Map<Long, Showtime> showtimeById = new HashMap<>();
    for (Showtime showtime : showtimeRepository.findAllById(showtimeIds)) {
      showtimeById.put(showtime.getId(), showtime);
    }

    List<Long> eventIds = showtimeById.values().stream().map(Showtime::getEventId).distinct().toList();
    List<Long> venueIds = showtimeById.values().stream().map(Showtime::getVenueId).distinct().toList();

    Map<Long, Event> eventById = new HashMap<>();
    for (Event event : eventRepository.findAllById(eventIds)) {
      eventById.put(event.getId(), event);
    }

    Map<Long, Venue> venueById = new HashMap<>();
    for (Venue venue : venueRepository.findAllById(venueIds)) {
      venueById.put(venue.getId(), venue);
    }

    List<ActiveLobbyDto> out = new ArrayList<>();
    for (Long showtimeId : showtimeIds) {
      Showtime showtime = showtimeById.get(showtimeId);
      if (showtime == null) {
        continue;
      }
      Event event = eventById.get(showtime.getEventId());
      Venue venue = venueById.get(showtime.getVenueId());
      out.add(new ActiveLobbyDto(
          showtimeId,
          event != null ? event.getTitle() : null,
          venue != null ? venue.getName() : null,
          showtime.getStartsAt(),
          lobbyPresenceRepository.countByShowtimeId(showtimeId)));
    }
    out.sort(Comparator.comparing(ActiveLobbyDto::startsAt, Comparator.nullsLast(Comparator.naturalOrder())));
    return out;
  }

  @Scheduled(fixedDelay = 60000)
  @Transactional
  public void cleanupStale() {
    Instant cutoff = Instant.now().minus(presenceTtlMinutes, ChronoUnit.MINUTES);
    lobbyPresenceRepository.deleteStaleNonTrekPresences(cutoff, EventType.TREK.name());
  }
}
