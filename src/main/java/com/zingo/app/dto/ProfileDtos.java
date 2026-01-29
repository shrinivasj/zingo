package com.zingo.app.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public class ProfileDtos {
  public record ProfileUpdateRequest(
      @Size(max = 80) String displayName,
      String avatarUrl,
      @Size(max = 140) String bioShort,
      List<String> personalityTags) {}
}
