package com.zingo.app.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public class ChatDtos {
  public record ConversationDto(
      Long id,
      Long showtimeId,
      String eventTitle,
      String venueName,
      Instant startsAt,
      List<Long> memberIds,
      String otherUserName,
      String otherUserAvatarUrl,
      String lastMessageText,
      Instant lastMessageAt) {}

  public record MessageDto(
      Long id,
      Long conversationId,
      Long senderId,
      String text,
      Instant createdAt) {}

  public record SendMessageRequest(@NotBlank String text) {}

  public record IcebreakerResponse(List<String> suggestions) {}
}
