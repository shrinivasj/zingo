package com.zingo.app.dto;

import jakarta.validation.constraints.NotBlank;

public class PushDtos {
  public record RegisterPushTokenRequest(
      @NotBlank String token,
      @NotBlank String platform) {}

  public record UnregisterPushTokenRequest(
      @NotBlank String token) {}
}
