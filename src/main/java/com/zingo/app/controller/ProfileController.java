package com.zingo.app.controller;

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
  public Profile me() {
    return profileService.getCurrentProfile();
  }

  @PutMapping("/me")
  public Profile update(@Valid @RequestBody ProfileUpdateRequest request) {
    return profileService.update(request);
  }
}
