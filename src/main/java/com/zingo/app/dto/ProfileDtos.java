package com.zingo.app.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public class ProfileDtos {
  public record ProfileMeResponse(
      Long userId,
      String displayName,
      String avatarUrl,
      String e2eePublicKey,
      String e2eeEncryptedPrivateKey,
      String e2eeKeySalt,
      boolean trekHostEnabled,
      String bioShort,
      List<String> personalityTags) {}

  public record ProfileUpdateRequest(
      @Size(max = 80) String displayName,
      String avatarUrl,
      @Size(max = 12000) String e2eePublicKey,
      @Size(max = 65535) String e2eeEncryptedPrivateKey,
      @Size(max = 255) String e2eeKeySalt,
      @Size(max = 140) String bioShort,
      List<String> personalityTags) {}
}
