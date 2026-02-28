package com.zingo.app.dto;

import com.zingo.app.entity.TrekJoinRequestStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public class TrekDtos {
  public record CreateTrekGroupRequest(
      @NotNull Long showtimeId,
      @Size(max = 280) String description,
      @Min(2) @Max(100) Integer maxMembers) {}

  public record TrekGroupDto(
      Long id,
      Long showtimeId,
      Long hostUserId,
      String hostDisplayName,
      String hostAvatarUrl,
      String description,
      Integer maxMembers,
      long pendingRequests,
      Instant createdAt) {}

  public record CreateTrekJoinRequest(@Size(max = 500) String note) {}

  public record TrekJoinRequestDto(
      Long id,
      Long groupId,
      Long showtimeId,
      Long requesterUserId,
      String requesterDisplayName,
      String requesterAvatarUrl,
      String note,
      TrekJoinRequestStatus status,
      Instant createdAt,
      Instant updatedAt,
      Instant reviewedAt) {}

  public record TrekDecisionResponse(TrekJoinRequestDto request, Long conversationId) {}

  public record TrekGroupListResponse(List<TrekGroupDto> groups) {}

  public record TrekHostStatusDto(boolean trekHostEnabled) {}
}
