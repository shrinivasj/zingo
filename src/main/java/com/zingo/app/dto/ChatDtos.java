package com.zingo.app.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ChatDtos {
  public record ConversationDto(
      Long id,
      Long showtimeId,
      String eventTitle,
      String eventPosterUrl,
      String venueName,
      Instant startsAt,
      List<Long> memberIds,
      List<String> participantNames,
      Map<Long, String> participantNameByUserId,
      Long otherUserId,
      String otherUserName,
      String otherUserAvatarUrl,
      String otherUserE2eePublicKey,
      String lastMessageText,
      Instant lastMessageAt) {}

  public record MessageDto(
      Long id,
      Long conversationId,
      Long senderId,
      String senderName,
      String text,
      Instant createdAt) {}

  public record SendMessageRequest(@NotBlank String text) {}

  public record IcebreakerResponse(List<String> suggestions) {}
}
