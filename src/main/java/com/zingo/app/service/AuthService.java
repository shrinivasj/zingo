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
    User user = userRepository.findByEmail(request.email().toLowerCase())
        .orElseThrow(() -> new BadRequestException("Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BadRequestException("Invalid credentials");
    }
    Profile profile = profileRepository.findById(user.getId())
        .orElseThrow(() -> new NotFoundException("Profile not found"));
    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthResponse(token, toUserDto(user, profile));
  }

  public UserDto me() {
    Long userId = SecurityUtil.currentUserId();
    if (userId == null) {
      throw new BadRequestException("Not authenticated");
    }
    User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    Profile profile = profileRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("Profile not found"));
    return toUserDto(user, profile);
  }

  public UserDto toUserDto(User user, Profile profile) {
    return new UserDto(
        user.getId(),
        user.getEmail(),
        profile.getDisplayName(),
        profile.getAvatarUrl(),
        profile.getBioShort(),
        profile.getPersonalityTags());
  }
}
