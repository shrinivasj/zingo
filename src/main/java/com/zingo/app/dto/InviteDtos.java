package com.zingo.app.dto;

import com.zingo.app.entity.InviteStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class InviteDtos {
  public record InviteRequest(@NotNull Long toUserId, @NotNull Long showtimeId) {}

  public record InviteDto(
      Long id,
      Long fromUserId,
      Long toUserId,
      Long showtimeId,
      InviteStatus status,
      Instant createdAt,
      Instant updatedAt) {}

  public record InviteAcceptResponse(InviteDto invite, Long conversationId) {}
}
