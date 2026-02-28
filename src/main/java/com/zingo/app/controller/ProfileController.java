package com.zingo.app.controller;

import com.zingo.app.dto.ProfileDtos.ProfileMeResponse;
import com.zingo.app.dto.ProfileDtos.ProfileUpdateRequest;
import com.zingo.app.entity.Profile;
import com.zingo.app.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
  private final ProfileService profileService;

  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping("/me")
  public ProfileMeResponse me() {
    return toProfileMeResponse(profileService.getCurrentProfile());
  }

  @PutMapping("/me")
  public ProfileMeResponse update(@Valid @RequestBody ProfileUpdateRequest request) {
    return toProfileMeResponse(profileService.update(request));
  }

  private ProfileMeResponse toProfileMeResponse(Profile profile) {
    return new ProfileMeResponse(
        profile.getUserId(),
        profile.getDisplayName(),
        profile.getAvatarUrl(),
        profile.getE2eePublicKey(),
        profile.getE2eeEncryptedPrivateKey(),
        profile.getE2eeKeySalt(),
        profile.isTrekHostEnabled(),
        profile.getBioShort(),
        profile.getPersonalityTags());
  }
}
