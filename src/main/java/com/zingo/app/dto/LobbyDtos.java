package com.zingo.app.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public class LobbyDtos {
  public record LobbyJoinRequest(@NotNull Long showtimeId) {}

  public record LobbyUserDto(
      Long userId,
      String displayName,
      String avatarUrl,
      String bioShort,
      List<String> personalityTags) {}

  public record LobbyUsersResponse(
      Long showtimeId,
      long total,
      List<LobbyUserDto> users,
      String eventType) {}

  public record ActiveLobbyDto(
      Long showtimeId,
      String eventTitle,
      String venueName,
      Instant startsAt,
      long liveCount) {}

  public record LobbyPresenceUpdate(Long showtimeId, long count, Instant updatedAt) {}
}
