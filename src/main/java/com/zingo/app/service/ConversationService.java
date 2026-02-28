package com.zingo.app.service;

import com.zingo.app.dto.ChatDtos.ConversationDto;
import com.zingo.app.dto.ChatDtos.MessageDto;
import com.zingo.app.entity.Conversation;
import com.zingo.app.entity.ConversationMember;
import com.zingo.app.entity.Event;
import com.zingo.app.entity.Message;
import com.zingo.app.entity.Profile;
import com.zingo.app.entity.Showtime;
import com.zingo.app.entity.Venue;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.exception.NotFoundException;
import com.zingo.app.repository.ConversationMemberRepository;
import com.zingo.app.repository.ConversationRepository;
import com.zingo.app.repository.EventRepository;
import com.zingo.app.repository.MessageRepository;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.repository.ShowtimeRepository;
import com.zingo.app.repository.VenueRepository;
import com.zingo.app.security.SecurityUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {
  private final ConversationRepository conversationRepository;
  private final ConversationMemberRepository conversationMemberRepository;
  private final MessageRepository messageRepository;
  private final ProfileRepository profileRepository;
  private final ShowtimeRepository showtimeRepository;
  private final EventRepository eventRepository;
  private final VenueRepository venueRepository;
  private final SafetyService safetyService;
  private final SimpMessagingTemplate messagingTemplate;
  private final PushDeliveryService pushDeliveryService;

  public ConversationService(ConversationRepository conversationRepository,
      ConversationMemberRepository conversationMemberRepository,
      MessageRepository messageRepository,
      ProfileRepository profileRepository,
      ShowtimeRepository showtimeRepository,
      EventRepository eventRepository,
      VenueRepository venueRepository,
      SafetyService safetyService,
      SimpMessagingTemplate messagingTemplate,
      PushDeliveryService pushDeliveryService) {
    this.conversationRepository = conversationRepository;
    this.conversationMemberRepository = conversationMemberRepository;
    this.messageRepository = messageRepository;
    this.profileRepository = profileRepository;
    this.showtimeRepository = showtimeRepository;
    this.eventRepository = eventRepository;
    this.venueRepository = venueRepository;
    this.safetyService = safetyService;
    this.messagingTemplate = messagingTemplate;
    this.pushDeliveryService = pushDeliveryService;
  }

  @Transactional
  public Long openConversation(Long showtimeId, Long userA, Long userB) {
    List<Conversation> existing = conversationRepository.findExistingByUsers(userA, userB);
    if (!existing.isEmpty()) {
      Conversation primary = existing.get(0);
      if (showtimeId != null && !showtimeId.equals(primary.getShowtimeId())) {
        primary.setShowtimeId(showtimeId);
        conversationRepository.save(primary);
      }

      // Deduplicate historic duplicate threads for the same pair by merging into primary.
      if (existing.size() > 1) {
        for (int i = 1; i < existing.size(); i++) {
          Conversation duplicate = existing.get(i);
          if (duplicate.getId().equals(primary.getId())) {
            continue;
          }
          messageRepository.moveConversationMessages(duplicate.getId(), primary.getId());
          conversationMemberRepository.deleteByConversationId(duplicate.getId());
          conversationRepository.deleteById(duplicate.getId());
        }
      }
      return primary.getId();
    }

    Conversation conversation = new Conversation();
    conversation.setShowtimeId(showtimeId);
    Conversation saved = conversationRepository.save(conversation);

    ConversationMember memberA = new ConversationMember();
    memberA.setConversationId(saved.getId());
    memberA.setUserId(userA);
    conversationMemberRepository.save(memberA);

    ConversationMember memberB = new ConversationMember();
    memberB.setConversationId(saved.getId());
    memberB.setUserId(userB);
    conversationMemberRepository.save(memberB);

    return saved.getId();
  }

  @Transactional
  public Long createGroupConversation(Long showtimeId, Long hostUserId) {
    Conversation conversation = new Conversation();
    conversation.setShowtimeId(showtimeId);
    Conversation saved = conversationRepository.save(conversation);

    ConversationMember hostMember = new ConversationMember();
    hostMember.setConversationId(saved.getId());
    hostMember.setUserId(hostUserId);
    conversationMemberRepository.save(hostMember);
    return saved.getId();
  }

  @Transactional
  public void addMemberToConversation(Long conversationId, Long userId) {
    if (conversationId == null || userId == null) {
      return;
    }
    if (conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
      return;
    }
    ConversationMember member = new ConversationMember();
    member.setConversationId(conversationId);
    member.setUserId(userId);
    conversationMemberRepository.save(member);
  }

  public List<ConversationDto> listForCurrentUser() {
    Long userId = SecurityUtil.currentUserId();
    List<Conversation> conversations = conversationRepository.findByMember(userId);
    if (conversations.isEmpty()) {
      return List.of();
    }

    List<Long> conversationIds = conversations.stream().map(Conversation::getId).toList();

    Map<Long, List<Long>> membersByConversation = new HashMap<>();
    Set<Long> otherUserIds = new HashSet<>();
    for (ConversationMember member : conversationMemberRepository.findByConversationIdIn(conversationIds)) {
      membersByConversation.computeIfAbsent(member.getConversationId(), key -> new ArrayList<>()).add(member.getUserId());
      if (!member.getUserId().equals(userId)) {
        otherUserIds.add(member.getUserId());
      }
    }

    Set<Long> showtimeIds = new HashSet<>();
    for (Conversation conversation : conversations) {
      showtimeIds.add(conversation.getShowtimeId());
    }
    Map<Long, Showtime> showtimeById = new HashMap<>();
    for (Showtime showtime : showtimeRepository.findAllById(showtimeIds)) {
      showtimeById.put(showtime.getId(), showtime);
    }

    Set<Long> eventIds = new HashSet<>();
    Set<Long> venueIds = new HashSet<>();
    for (Showtime showtime : showtimeById.values()) {
      eventIds.add(showtime.getEventId());
      venueIds.add(showtime.getVenueId());
    }

    Map<Long, Event> eventById = new HashMap<>();
    for (Event event : eventRepository.findAllById(eventIds)) {
      eventById.put(event.getId(), event);
    }

    Map<Long, Venue> venueById = new HashMap<>();
    for (Venue venue : venueRepository.findAllById(venueIds)) {
      venueById.put(venue.getId(), venue);
    }

    Map<Long, Profile> profileByUserId = new HashMap<>();
    if (!otherUserIds.isEmpty()) {
      for (Profile profile : profileRepository.findAllById(otherUserIds)) {
        profileByUserId.put(profile.getUserId(), profile);
      }
    }

    Map<Long, Message> lastMessageByConversation = new HashMap<>();
    for (Message message : messageRepository.findLatestByConversationIds(conversationIds)) {
      lastMessageByConversation.put(message.getConversationId(), message);
    }

    List<ConversationDto> dtos = new ArrayList<>();
    for (Conversation conversation : conversations) {
      Showtime showtime = showtimeById.get(conversation.getShowtimeId());
      Event event = showtime != null ? eventById.get(showtime.getEventId()) : null;
      Venue venue = showtime != null ? venueById.get(showtime.getVenueId()) : null;
      List<Long> members = membersByConversation.getOrDefault(conversation.getId(), List.of());
      List<String> participantNames = members.stream()
          .filter(id -> !id.equals(userId))
          .map(profileByUserId::get)
          .filter(java.util.Objects::nonNull)
          .map(Profile::getDisplayName)
          .filter(name -> name != null && !name.isBlank())
          .distinct()
          .toList();
      Map<Long, String> participantNameByUserId = new HashMap<>();
      for (Long memberId : members) {
        if (memberId == null) {
          continue;
        }
        if (memberId.equals(userId)) {
          Profile self = profileRepository.findById(memberId).orElse(null);
          participantNameByUserId.put(memberId, self != null ? self.getDisplayName() : "You");
          continue;
        }
        Profile memberProfile = profileByUserId.get(memberId);
        if (memberProfile != null && memberProfile.getDisplayName() != null && !memberProfile.getDisplayName().isBlank()) {
          participantNameByUserId.put(memberId, memberProfile.getDisplayName());
        }
      }
      Long otherUserId = members.stream().filter(id -> !id.equals(userId)).findFirst().orElse(null);
      Profile otherProfile = otherUserId != null ? profileByUserId.get(otherUserId) : null;
      Message lastMessage = lastMessageByConversation.get(conversation.getId());
      dtos.add(new ConversationDto(
          conversation.getId(),
          conversation.getShowtimeId(),
          event != null ? event.getTitle() : null,
          event != null ? event.getPosterUrl() : null,
          venue != null ? venue.getName() : null,
          showtime != null ? showtime.getStartsAt() : null,
          members,
          participantNames,
          participantNameByUserId,
          otherUserId,
          otherProfile != null ? otherProfile.getDisplayName() : null,
          otherProfile != null ? otherProfile.getAvatarUrl() : null,
          otherProfile != null ? otherProfile.getE2eePublicKey() : null,
          lastMessage != null ? summarizeLastMessage(lastMessage.getText()) : null,
          lastMessage != null ? lastMessage.getCreatedAt() : null));
    }
    dtos.sort((a, b) -> {
      boolean aHasLast = a.lastMessageAt() != null;
      boolean bHasLast = b.lastMessageAt() != null;
      if (aHasLast != bHasLast) {
        return bHasLast ? 1 : -1;
      }

      if (aHasLast && bHasLast) {
        int byLast = Comparator.<java.time.Instant>nullsLast(Comparator.naturalOrder())
            .compare(b.lastMessageAt(), a.lastMessageAt());
        if (byLast != 0) {
          return byLast;
        }
      } else {
        int byStart = Comparator.<java.time.Instant>nullsLast(Comparator.naturalOrder())
            .compare(b.startsAt(), a.startsAt());
        if (byStart != 0) {
          return byStart;
        }
      }
      return Comparator.<Long>nullsLast(Comparator.reverseOrder()).compare(a.id(), b.id());
    });
    return dtos;
  }

  public List<MessageDto> listMessages(Long conversationId, int page, int size) {
    Long userId = SecurityUtil.currentUserId();
    ensureMember(conversationId, userId);
    List<Message> pageItems = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(page, size));
    Collections.reverse(pageItems);
    Set<Long> senderIds = new HashSet<>();
    for (Message message : pageItems) {
      if (message.getSenderId() != null) {
        senderIds.add(message.getSenderId());
      }
    }
    Map<Long, String> senderNameById = new HashMap<>();
    for (Profile profile : profileRepository.findAllById(senderIds)) {
      senderNameById.put(profile.getUserId(), profile.getDisplayName());
    }
    return pageItems.stream().map((message) -> toDto(message, senderNameById.get(message.getSenderId()))).toList();
  }

  @Transactional
  public MessageDto sendMessage(Long conversationId, String text) {
    Long userId = SecurityUtil.currentUserId();
    ensureMember(conversationId, userId);
    List<Long> members = conversationMemberRepository.findByConversationId(conversationId).stream()
        .map(ConversationMember::getUserId)
        .toList();
    Long otherUser = members.stream().filter(id -> !id.equals(userId)).findFirst().orElse(null);
    if (otherUser != null && safetyService.isBlockedBetween(userId, otherUser)) {
      throw new BadRequestException("Cannot chat with this user");
    }

    Message message = new Message();
    message.setConversationId(conversationId);
    message.setSenderId(userId);
    message.setText(text);
    Message saved = messageRepository.save(message);

    Profile sender = profileRepository.findById(userId).orElse(null);
    String senderName = sender != null && sender.getDisplayName() != null ? sender.getDisplayName() : "Someone";
    MessageDto dto = toDto(saved, senderName);
    messagingTemplate.convertAndSend("/topic/chat." + conversationId, dto);
    if (otherUser != null) {
      String preview = summarizeLastMessage(text);
      Map<String, Object> data = new HashMap<>();
      data.put("pushType", "CHAT");
      data.put("conversationId", conversationId);
      data.put("senderId", userId);
      pushDeliveryService.sendToUser(otherUser, "New message from " + senderName, preview, data);
    }
    return dto;
  }

  private void ensureMember(Long conversationId, Long userId) {
    if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
      throw new NotFoundException("Conversation not found");
    }
  }

  public MessageDto toDto(Message message) {
    String senderName = profileRepository.findById(message.getSenderId())
        .map(Profile::getDisplayName)
        .orElse(null);
    return toDto(message, senderName);
  }

  private MessageDto toDto(Message message, String senderName) {
    return new MessageDto(message.getId(), message.getConversationId(), message.getSenderId(), senderName, message.getText(),
        message.getCreatedAt());
  }

  private String summarizeLastMessage(String text) {
    if (text == null) {
      return null;
    }
    if (text.startsWith("enc:v1:")) {
      return "Encrypted message";
    }
    return text;
  }

  @Transactional
  public void leaveConversation(Long conversationId) {
    Long userId = SecurityUtil.currentUserId();
    ensureMember(conversationId, userId);
    conversationMemberRepository.deleteByConversationIdAndUserId(conversationId, userId);
    long remaining = conversationMemberRepository.countByConversationId(conversationId);
    if (remaining == 0) {
      messageRepository.deleteByConversationId(conversationId);
      conversationRepository.deleteById(conversationId);
    }
  }
}
