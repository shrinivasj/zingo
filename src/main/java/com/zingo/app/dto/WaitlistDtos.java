package com.zingo.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WaitlistDtos {
  public record JoinWaitlistRequest(
      @NotBlank @Email @Size(max = 254) String email,
      @Size(max = 80) String name,
      // Honeypot: bots fill hidden fields. If present, accept but do not store.
      @Size(max = 200) String company) {}

  public record JoinWaitlistResponse(boolean ok, String status, String message) {}
}

