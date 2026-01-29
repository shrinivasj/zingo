package com.zingo.app.service;

import com.zingo.app.dto.ProfileDtos.ProfileUpdateRequest;
import com.zingo.app.entity.Profile;
import com.zingo.app.exception.NotFoundException;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.security.SecurityUtil;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
  private final ProfileRepository profileRepository;

  public ProfileService(ProfileRepository profileRepository) {
    this.profileRepository = profileRepository;
  }

  public Profile getCurrentProfile() {
    Long userId = SecurityUtil.currentUserId();
    return profileRepository.findById(userId).orElseThrow(() -> new NotFoundException("Profile not found"));
  }

  @Transactional
  public Profile update(ProfileUpdateRequest request) {
    Profile profile = getCurrentProfile();
    if (request.displayName() != null) {
      profile.setDisplayName(request.displayName());
    }
    if (request.avatarUrl() != null) {
      profile.setAvatarUrl(request.avatarUrl());
    }
    if (request.bioShort() != null) {
      profile.setBioShort(request.bioShort());
    }
    if (request.personalityTags() != null) {
      profile.setPersonalityTags(List.copyOf(request.personalityTags()));
    }
    return profileRepository.save(profile);
  }
}
