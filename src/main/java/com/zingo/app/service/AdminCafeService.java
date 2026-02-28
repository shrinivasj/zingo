package com.zingo.app.service;

import com.zingo.app.dto.AdminCafeDtos.CreateCafePlanRequest;
import com.zingo.app.dto.AdminCafeDtos.CreateCafePlanResponse;
import com.zingo.app.dto.AdminCafeDtos.CreateTrekPlanRequest;
import com.zingo.app.entity.City;
import com.zingo.app.entity.Conversation;
import com.zingo.app.entity.Event;
import com.zingo.app.entity.EventType;
import com.zingo.app.entity.Invite;
import com.zingo.app.entity.ShowFormat;
import com.zingo.app.entity.Showtime;
import com.zingo.app.entity.TrekGroup;
import com.zingo.app.entity.Venue;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.exception.NotFoundException;
import com.zingo.app.repository.CityRepository;
import com.zingo.app.repository.ConversationMemberRepository;
import com.zingo.app.repository.ConversationRepository;
import com.zingo.app.repository.EventRepository;
import com.zingo.app.repository.InviteRepository;
import com.zingo.app.repository.LobbyPresenceRepository;
import com.zingo.app.repository.MessageRepository;
import com.zingo.app.repository.ShowtimeRepository;
import com.zingo.app.repository.TrekGroupRepository;
import com.zingo.app.repository.TrekJoinRequestRepository;
import com.zingo.app.repository.VenueRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCafeService {
  private final CityRepository cityRepository;
  private final VenueRepository venueRepository;
  private final EventRepository eventRepository;
  private final ShowtimeRepository showtimeRepository;
  private final LobbyPresenceRepository lobbyPresenceRepository;
  private final InviteRepository inviteRepository;
  private final ConversationRepository conversationRepository;
  private final ConversationMemberRepository conversationMemberRepository;
  private final MessageRepository messageRepository;
  private final TrekGroupRepository trekGroupRepository;
  private final TrekJoinRequestRepository trekJoinRequestRepository;
  private final AdminAuditService adminAuditService;

  public AdminCafeService(CityRepository cityRepository, VenueRepository venueRepository, EventRepository eventRepository,
      ShowtimeRepository showtimeRepository, LobbyPresenceRepository lobbyPresenceRepository,
      InviteRepository inviteRepository, ConversationRepository conversationRepository,
      ConversationMemberRepository conversationMemberRepository, MessageRepository messageRepository,
      TrekGroupRepository trekGroupRepository, TrekJoinRequestRepository trekJoinRequestRepository,
      AdminAuditService adminAuditService) {
    this.cityRepository = cityRepository;
    this.venueRepository = venueRepository;
    this.eventRepository = eventRepository;
    this.showtimeRepository = showtimeRepository;
    this.lobbyPresenceRepository = lobbyPresenceRepository;
    this.inviteRepository = inviteRepository;
    this.conversationRepository = conversationRepository;
    this.conversationMemberRepository = conversationMemberRepository;
    this.messageRepository = messageRepository;
    this.trekGroupRepository = trekGroupRepository;
    this.trekJoinRequestRepository = trekJoinRequestRepository;
    this.adminAuditService = adminAuditService;
  }

  @Transactional(readOnly = true)
  public List<CreateCafePlanResponse> listCafePlans() {
    return listPlans(EventType.CAFE);
  }

  @Transactional(readOnly = true)
  public List<CreateCafePlanResponse> listTrekPlans() {
    return listPlans(EventType.TREK);
  }

  @Transactional
  public CreateCafePlanResponse createCafePlan(CreateCafePlanRequest request) {
    CreateCafePlanResponse response = createPlan(
        request != null ? request.cityId() : null,
        request != null ? request.venueName() : null,
        request != null ? request.title() : null,
        request != null ? request.startsAt() : null,
        request != null ? request.address() : null,
        request != null ? request.postalCode() : null,
        EventType.CAFE);
    adminAuditService.logActivity("CAFE_PLAN_CREATED", "New cafe plan created: \"" + response.title() + "\"",
        response.venueName() + ", " + response.cityName());
    return response;
  }

  @Transactional
  public CreateCafePlanResponse createTrekPlan(CreateTrekPlanRequest request) {
    CreateCafePlanResponse response = createPlan(
        request != null ? request.cityId() : null,
        request != null ? request.venueName() : null,
        request != null ? request.title() : null,
        request != null ? request.startsAt() : null,
        request != null ? request.address() : null,
        request != null ? request.postalCode() : null,
        EventType.TREK);
    adminAuditService.logActivity("TREK_PLAN_CREATED", "Trek plan published: \"" + response.title() + "\"",
        response.venueName() + ", " + response.cityName());
    return response;
  }

  @Transactional
  public CreateCafePlanResponse updateCafePlan(Long showtimeId, CreateCafePlanRequest request) {
    CreateCafePlanResponse response = updatePlan(showtimeId,
        request != null ? request.cityId() : null,
        request != null ? request.venueName() : null,
        request != null ? request.title() : null,
        request != null ? request.startsAt() : null,
        request != null ? request.address() : null,
        request != null ? request.postalCode() : null,
        EventType.CAFE);
    adminAuditService.logActivity("CAFE_PLAN_UPDATED", "Cafe plan updated: \"" + response.title() + "\"",
        response.venueName() + ", " + response.cityName());
    return response;
  }

  @Transactional
  public CreateCafePlanResponse updateTrekPlan(Long showtimeId, CreateTrekPlanRequest request) {
    CreateCafePlanResponse response = updatePlan(showtimeId,
        request != null ? request.cityId() : null,
        request != null ? request.venueName() : null,
        request != null ? request.title() : null,
        request != null ? request.startsAt() : null,
        request != null ? request.address() : null,
        request != null ? request.postalCode() : null,
        EventType.TREK);
    adminAuditService.logActivity("TREK_PLAN_UPDATED", "Trek plan updated: \"" + response.title() + "\"",
        response.venueName() + ", " + response.cityName());
    return response;
  }

  @Transactional
  public void deleteCafePlan(Long showtimeId) {
    String summary = deletePlan(showtimeId, EventType.CAFE);
    adminAuditService.logActivity("CAFE_PLAN_DELETED", "Cafe plan deleted", summary);
  }

  @Transactional
  public void deleteTrekPlan(Long showtimeId) {
    String summary = deletePlan(showtimeId, EventType.TREK);
    adminAuditService.logActivity("TREK_PLAN_DELETED", "Trek plan deleted", summary);
  }

  private List<CreateCafePlanResponse> listPlans(EventType eventType) {
    List<Showtime> showtimes = showtimeRepository.findByAdminEventType(eventType);
    if (showtimes.isEmpty()) {
      return List.of();
    }

    Set<Long> eventIds = showtimes.stream().map(Showtime::getEventId).collect(Collectors.toSet());
    Set<Long> venueIds = showtimes.stream().map(Showtime::getVenueId).collect(Collectors.toSet());

    Map<Long, Event> eventById = new HashMap<>();
    for (Event event : eventRepository.findAllById(eventIds)) {
      eventById.put(event.getId(), event);
    }
    Map<Long, Venue> venueById = new HashMap<>();
    for (Venue venue : venueRepository.findAllById(venueIds)) {
      venueById.put(venue.getId(), venue);
    }
    Set<Long> cityIds = venueById.values().stream().map(Venue::getCityId).collect(Collectors.toSet());
    Map<Long, City> cityById = new HashMap<>();
    for (City city : cityRepository.findAllById(cityIds)) {
      cityById.put(city.getId(), city);
    }

    List<CreateCafePlanResponse> out = new ArrayList<>();
    for (Showtime showtime : showtimes) {
      Event event = eventById.get(showtime.getEventId());
      Venue venue = venueById.get(showtime.getVenueId());
      if (event == null || venue == null) {
        continue;
      }
      City city = cityById.get(venue.getCityId());
      out.add(toResponse(city, venue, event, showtime, false, false, false));
    }
    out.sort(Comparator.comparing(CreateCafePlanResponse::startsAt, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(CreateCafePlanResponse::showtimeId, Comparator.reverseOrder()));
    return out;
  }

  private CreateCafePlanResponse createPlan(
      Long cityId,
      String venueNameRaw,
      String titleRaw,
      Instant startsAtRaw,
      String addressRaw,
      String postalCodeRaw,
      EventType eventType) {
    ValidatedPlanInput input = validateAndNormalize(cityId, venueNameRaw, titleRaw, startsAtRaw, eventType);

    City city = cityRepository.findById(input.cityId())
        .orElseThrow(() -> new NotFoundException("City not found"));

    boolean venueCreated = false;
    Venue venue = venueRepository.findFirstByCityIdAndNameIgnoreCase(city.getId(), input.venueName()).orElse(null);
    if (venue == null) {
      venue = new Venue();
      venue.setCityId(city.getId());
      venue.setName(input.venueName());
      venueCreated = true;
    }
    applyVenueDetails(venue, city, addressRaw, postalCodeRaw);
    venue = venueRepository.save(venue);

    boolean eventCreated = false;
    Event event = eventRepository.findFirstByTitleIgnoreCaseAndType(input.title(), eventType).orElse(null);
    if (event == null) {
      event = new Event();
      event.setType(eventType);
      event.setTitle(input.title());
      eventCreated = true;
      event = eventRepository.save(event);
    }

    boolean showtimeCreated = false;
    Showtime showtime = showtimeRepository.findFirstByEventIdAndVenueIdAndStartsAtAndFormat(
        event.getId(), venue.getId(), input.startsAt(), ShowFormat.GENERAL).orElse(null);
    if (showtime == null) {
      showtime = new Showtime();
      showtime.setEventId(event.getId());
      showtime.setVenueId(venue.getId());
      showtime.setStartsAt(input.startsAt());
      showtime.setFormat(ShowFormat.GENERAL);
      showtime = showtimeRepository.save(showtime);
      showtimeCreated = true;
    }

    return toResponse(city, venue, event, showtime, venueCreated, eventCreated, showtimeCreated);
  }

  private CreateCafePlanResponse updatePlan(
      Long showtimeId,
      Long cityId,
      String venueNameRaw,
      String titleRaw,
      Instant startsAtRaw,
      String addressRaw,
      String postalCodeRaw,
      EventType eventType) {
    if (showtimeId == null) {
      throw new BadRequestException("showtimeId is required");
    }
    Showtime showtime = showtimeRepository.findById(showtimeId)
        .orElseThrow(() -> new NotFoundException("Plan not found"));
    Event currentEvent = eventRepository.findById(showtime.getEventId())
        .orElseThrow(() -> new NotFoundException("Plan event not found"));
    if (currentEvent.getType() != eventType) {
      throw new BadRequestException("Plan type mismatch");
    }
    ValidatedPlanInput input = validateAndNormalize(cityId, venueNameRaw, titleRaw, startsAtRaw, eventType);
    City city = cityRepository.findById(input.cityId())
        .orElseThrow(() -> new NotFoundException("City not found"));

    Long oldEventId = showtime.getEventId();
    Long oldVenueId = showtime.getVenueId();

    Venue targetVenue = venueRepository.findFirstByCityIdAndNameIgnoreCase(city.getId(), input.venueName()).orElse(null);
    if (targetVenue == null) {
      targetVenue = new Venue();
      targetVenue.setCityId(city.getId());
      targetVenue.setName(input.venueName());
    }
    applyVenueDetails(targetVenue, city, addressRaw, postalCodeRaw);
    targetVenue = venueRepository.save(targetVenue);

    Event targetEvent = eventRepository.findFirstByTitleIgnoreCaseAndType(input.title(), eventType).orElse(null);
    if (targetEvent == null) {
      targetEvent = new Event();
      targetEvent.setType(eventType);
      targetEvent.setTitle(input.title());
      targetEvent = eventRepository.save(targetEvent);
    }

    showtime.setVenueId(targetVenue.getId());
    showtime.setEventId(targetEvent.getId());
    showtime.setStartsAt(input.startsAt());
    showtime.setFormat(ShowFormat.GENERAL);
    showtime = showtimeRepository.save(showtime);

    cleanupOrphanEvent(oldEventId, showtime.getEventId());
    cleanupOrphanVenue(oldVenueId, showtime.getVenueId());

    return toResponse(city, targetVenue, targetEvent, showtime, false, false, false);
  }

  private String deletePlan(Long showtimeId, EventType eventType) {
    if (showtimeId == null) {
      throw new BadRequestException("showtimeId is required");
    }
    Showtime showtime = showtimeRepository.findById(showtimeId)
        .orElseThrow(() -> new NotFoundException("Plan not found"));
    Event event = eventRepository.findById(showtime.getEventId())
        .orElseThrow(() -> new NotFoundException("Plan event not found"));
    Venue venue = venueRepository.findById(showtime.getVenueId()).orElse(null);
    if (event.getType() != eventType) {
      throw new BadRequestException("Plan type mismatch");
    }

    Long eventId = showtime.getEventId();
    Long venueId = showtime.getVenueId();

    lobbyPresenceRepository.deleteByShowtimeId(showtimeId);

    List<Invite> invites = inviteRepository.findByShowtimeId(showtimeId);
    if (!invites.isEmpty()) {
      inviteRepository.deleteAll(invites);
    }

    List<Conversation> conversations = conversationRepository.findByShowtimeId(showtimeId);
    for (Conversation conversation : conversations) {
      messageRepository.deleteByConversationId(conversation.getId());
      conversationMemberRepository.deleteByConversationId(conversation.getId());
    }
    if (!conversations.isEmpty()) {
      conversationRepository.deleteAll(conversations);
    }

    List<TrekGroup> groups = trekGroupRepository.findByShowtimeIdOrderByCreatedAtAsc(showtimeId);
    if (!groups.isEmpty()) {
      List<Long> groupIds = groups.stream().map(TrekGroup::getId).toList();
      trekJoinRequestRepository.deleteByGroupIdIn(groupIds);
      trekGroupRepository.deleteAll(groups);
    }

    showtimeRepository.delete(showtime);
    cleanupOrphanEvent(eventId, null);
    cleanupOrphanVenue(venueId, null);
    return event.getTitle() + (venue != null ? " | " + venue.getName() : "");
  }

  private void cleanupOrphanEvent(Long candidateEventId, Long keepEventId) {
    if (candidateEventId == null || candidateEventId.equals(keepEventId)) {
      return;
    }
    if (showtimeRepository.countByEventId(candidateEventId) == 0) {
      eventRepository.deleteById(candidateEventId);
    }
  }

  private void cleanupOrphanVenue(Long candidateVenueId, Long keepVenueId) {
    if (candidateVenueId == null || candidateVenueId.equals(keepVenueId)) {
      return;
    }
    if (showtimeRepository.countByVenueId(candidateVenueId) == 0) {
      venueRepository.deleteById(candidateVenueId);
    }
  }

  private void applyVenueDetails(Venue venue, City city, String addressRaw, String postalCodeRaw) {
    if (trimToNull(addressRaw) != null) {
      venue.setAddress(addressRaw.trim());
    }
    if (trimToNull(postalCodeRaw) != null) {
      venue.setPostalCode(postalCodeRaw.trim());
    } else if (venue.getPostalCode() == null && city.getPostalCode() != null) {
      venue.setPostalCode(city.getPostalCode());
    }
  }

  private CreateCafePlanResponse toResponse(City city, Venue venue, Event event, Showtime showtime,
      boolean venueCreated, boolean eventCreated, boolean showtimeCreated) {
    return new CreateCafePlanResponse(
        city != null ? city.getId() : venue.getCityId(),
        city != null ? city.getName() : null,
        venue.getId(),
        venue.getName(),
        event.getId(),
        event.getTitle(),
        showtime.getId(),
        showtime.getStartsAt(),
        venue.getAddress(),
        venue.getPostalCode(),
        event.getType(),
        venueCreated,
        eventCreated,
        showtimeCreated);
  }

  private ValidatedPlanInput validateAndNormalize(Long cityId, String venueNameRaw, String titleRaw,
      Instant startsAtRaw, EventType eventType) {
    if (cityId == null) {
      throw new BadRequestException("cityId is required");
    }
    String venueName = trimToNull(venueNameRaw);
    String title = trimToNull(titleRaw);
    Instant startsAt = startsAtRaw;
    if (venueName == null) {
      throw new BadRequestException("venueName is required");
    }
    if (title == null) {
      title = venueName;
    }
    if (startsAt == null) {
      startsAt = eventType == EventType.TREK ? defaultTrekStartsAt() : defaultCafeStartsAt();
    }
    return new ValidatedPlanInput(cityId, venueName, title, startsAt);
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private Instant defaultCafeStartsAt() {
    Instant now = Instant.now();
    Instant nextHour = now.truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS);
    return nextHour.plus(1, ChronoUnit.HOURS);
  }

  private Instant defaultTrekStartsAt() {
    LocalDateTime nextMorning = LocalDateTime.now(ZoneOffset.UTC)
        .plusDays(1)
        .with(LocalTime.of(6, 0))
        .truncatedTo(ChronoUnit.MINUTES);
    return nextMorning.toInstant(ZoneOffset.UTC);
  }

  private record ValidatedPlanInput(Long cityId, String venueName, String title, Instant startsAt) {}
}
