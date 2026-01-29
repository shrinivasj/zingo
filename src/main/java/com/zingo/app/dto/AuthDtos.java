package com.zingo.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class AuthDtos {
  public record RegisterRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(min = 6, max = 100) String password,
      @NotBlank @Size(max = 80) String displayName) {}

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

  public record AuthResponse(String token, UserDto user) {}

  public record UserDto(
      Long id,
      String email,
      String displayName,
      String avatarUrl,
      String bioShort,
      List<String> personalityTags) {}
}
