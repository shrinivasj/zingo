package com.zingo.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SafetyDtos {
  public record BlockRequest(@NotNull Long blockedId) {}

  public record ReportRequest(
      @NotNull Long reportedId,
      @NotBlank @Size(max = 120) String reason,
      @Size(max = 1000) String details) {}
}
