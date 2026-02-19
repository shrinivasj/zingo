package com.zingo.app.service;

import com.zingo.app.dto.AuthDtos;
import com.zingo.app.dto.AuthDtos.AuthResponse;
import com.zingo.app.dto.AuthDtos.LoginRequest;
import com.zingo.app.dto.AuthDtos.RegisterRequest;
import com.zingo.app.dto.AuthDtos.UserDto;
import com.zingo.app.entity.Profile;
import com.zingo.app.entity.User;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.exception.NotFoundException;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.repository.UserRepository;
import com.zingo.app.security.JwtService;
import com.zingo.app.security.SecurityUtil;
import java.util.Collections;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(UserRepository userRepository, ProfileRepository profileRepository, PasswordEncoder passwordEncoder,
      JwtService jwtService) {
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email().toLowerCase())) {
      throw new BadRequestException("Email already registered");
    }
    User user = new User();
    user.setEmail(request.email().toLowerCase());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user = userRepository.save(user);

    Profile profile = new Profile();
    profile.setUserId(user.getId());
    profile.setDisplayName(request.displayName());
    profile.setAvatarUrl(null);
    profile.setBioShort(null);
    profile.setPersonalityTags(Collections.emptyList());
    profileRepository.save(profile);

    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthDtos.AuthResponse(token, toUserDto(user, profile));
  }

  public AuthResponse login(LoginRequest request) {
    String email = request.email().toLowerCase();
    Object[] row = userRepository.findUserWithProfileByEmail(email)
        .orElseThrow(() -> new BadRequestException("Invalid credentials"));
    UserProfilePair pair = unpackUserProfile(row);
    User user = pair.user();
    Profile profile = pair.profile();
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadRequestException("Invalid credentials");
    }
    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthResponse(token, toUserDto(user, profile));
  }

  public UserDto me() {
    JwtService.JwtUser jwtUser = SecurityUtil.currentJwtUser();
    if (jwtUser == null || jwtUser.userId() == null) {
      throw new BadRequestException("Not authenticated");
    }
    Long userId = jwtUser.userId();
    Profile profile = profileRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("Profile not found"));

    String email = jwtUser.email();
    if (email == null || email.isBlank()) {
      User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
      email = user.getEmail();
    }

    return new UserDto(
        userId,
        email,
        profile.getDisplayName(),
        sanitizeAuthAvatar(profile.getAvatarUrl()),
        profile.getBioShort(),
        profile.getPersonalityTags());
  }

  public UserDto toUserDto(User user, Profile profile) {
    return new UserDto(
        user.getId(),
        user.getEmail(),
        profile.getDisplayName(),
        sanitizeAuthAvatar(profile.getAvatarUrl()),
        profile.getBioShort(),
        profile.getPersonalityTags());
  }

  private String sanitizeAuthAvatar(String avatarUrl) {
    if (avatarUrl == null || avatarUrl.isBlank()) {
      return avatarUrl;
    }
    // Prevent large inline base64 image payloads from slowing auth responses.
    if (avatarUrl.startsWith("data:") || avatarUrl.length() > 2048) {
      return null;
    }
    return avatarUrl;
  }

  private UserProfilePair unpackUserProfile(Object[] row) {
    if (row.length >= 2 && row[0] instanceof User user && row[1] instanceof Profile profile) {
      return new UserProfilePair(user, profile);
    }
    if (row.length >= 1 && row[0] instanceof Object[] nested
        && nested.length >= 2
        && nested[0] instanceof User user
        && nested[1] instanceof Profile profile) {
      return new UserProfilePair(user, profile);
    }
    throw new NotFoundException("Profile not found");
  }

  private record UserProfilePair(User user, Profile profile) {}
}
