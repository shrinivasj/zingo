package com.zingo.app.service;

import com.zingo.app.dto.AuthDtos;
import com.zingo.app.dto.AuthDtos.AuthResponse;
import com.zingo.app.dto.AuthDtos.EmailOtpSendRequest;
import com.zingo.app.dto.AuthDtos.EmailOtpSendResponse;
import com.zingo.app.dto.AuthDtos.EmailOtpVerifyRequest;
import com.zingo.app.dto.AuthDtos.LoginRequest;
import com.zingo.app.dto.AuthDtos.RegisterRequest;
import com.zingo.app.dto.AuthDtos.UserDto;
import com.zingo.app.entity.EmailOtpChallenge;
import com.zingo.app.entity.Profile;
import com.zingo.app.entity.User;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.exception.NotFoundException;
import com.zingo.app.exception.TooManyRequestsException;
import com.zingo.app.repository.EmailOtpChallengeRepository;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.repository.UserRepository;
import com.zingo.app.security.JwtService;
import com.zingo.app.security.SecurityUtil;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private static final String EMAIL_LOGIN_PURPOSE = "EMAIL_LOGIN";

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final EmailOtpChallengeRepository emailOtpChallengeRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final EmailOtpSender emailOtpSender;
  private final SecureRandom secureRandom = new SecureRandom();
  private final long otpExpirationMinutes;
  private final long otpResendCooldownSeconds;
  private final int otpMaxAttempts;
  private final boolean otpDevExposeCode;

  public AuthService(
      UserRepository userRepository,
      ProfileRepository profileRepository,
      EmailOtpChallengeRepository emailOtpChallengeRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      EmailOtpSender emailOtpSender,
      @Value("${app.auth.otp.expirationMinutes}") long otpExpirationMinutes,
      @Value("${app.auth.otp.resendCooldownSeconds}") long otpResendCooldownSeconds,
      @Value("${app.auth.otp.maxAttempts}") int otpMaxAttempts,
      @Value("${app.auth.otp.devExposeCode:false}") boolean otpDevExposeCode) {
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
    this.emailOtpChallengeRepository = emailOtpChallengeRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.emailOtpSender = emailOtpSender;
    this.otpExpirationMinutes = otpExpirationMinutes;
    this.otpResendCooldownSeconds = otpResendCooldownSeconds;
    this.otpMaxAttempts = otpMaxAttempts;
    this.otpDevExposeCode = otpDevExposeCode;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmail(email)) {
      throw new BadRequestException("Email already registered");
    }
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user = userRepository.save(user);

    Profile profile = createProfile(user.getId(), request.displayName());

    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthDtos.AuthResponse(token, toUserDto(user, profile));
  }

  public AuthResponse login(LoginRequest request) {
    String email = normalizeEmail(request.email());
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

  @Transactional
  public EmailOtpSendResponse requestEmailOtp(EmailOtpSendRequest request) {
    String email = normalizeEmail(request.email());
    Instant now = Instant.now();

    Optional<EmailOtpChallenge> latestOpt = emailOtpChallengeRepository
        .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, EMAIL_LOGIN_PURPOSE);
    if (latestOpt.isPresent()) {
      EmailOtpChallenge latest = latestOpt.get();
      if (latest.getConsumedAt() == null) {
        long elapsedSeconds = Duration.between(latest.getCreatedAt(), now).getSeconds();
        long remainingCooldown = otpResendCooldownSeconds - Math.max(0, elapsedSeconds);
        if (remainingCooldown > 0) {
          throw new TooManyRequestsException("Please wait " + remainingCooldown + " seconds before requesting another code");
        }
      }
    }

    String code = generateOtpCode();
    EmailOtpChallenge challenge = new EmailOtpChallenge();
    challenge.setEmail(email);
    challenge.setPurpose(EMAIL_LOGIN_PURPOSE);
    challenge.setCodeHash(passwordEncoder.encode(code));
    challenge.setAttemptCount(0);
    challenge.setMaxAttempts(otpMaxAttempts);
    challenge.setExpiresAt(now.plus(Duration.ofMinutes(otpExpirationMinutes)));
    emailOtpChallengeRepository.save(challenge);

    emailOtpSender.sendLoginCode(email, code, otpExpirationMinutes);
    return new EmailOtpSendResponse(
        email,
        Math.toIntExact(Duration.ofMinutes(otpExpirationMinutes).getSeconds()),
        Math.toIntExact(otpResendCooldownSeconds),
        otpDevExposeCode ? code : null);
  }

  @Transactional
  public AuthResponse verifyEmailOtp(EmailOtpVerifyRequest request) {
    String email = normalizeEmail(request.email());
    String code = request.code().trim();
    Instant now = Instant.now();

    EmailOtpChallenge challenge = emailOtpChallengeRepository
        .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, EMAIL_LOGIN_PURPOSE)
        .orElseThrow(() -> new BadRequestException("Request a code first"));

    if (challenge.getConsumedAt() != null) {
      throw new BadRequestException("This code has already been used. Request a new one");
    }
    if (challenge.getExpiresAt().isBefore(now)) {
      throw new BadRequestException("This code has expired. Request a new one");
    }
    if (challenge.getAttemptCount() >= challenge.getMaxAttempts()) {
      throw new BadRequestException("Too many incorrect attempts. Request a new code");
    }
    if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
      challenge.setAttemptCount(challenge.getAttemptCount() + 1);
      emailOtpChallengeRepository.save(challenge);
      throw new BadRequestException("Invalid code");
    }

    challenge.setConsumedAt(now);
    emailOtpChallengeRepository.save(challenge);

    User user = userRepository.findByEmail(email).orElseGet(() -> createOtpUser(email, request.displayName()));
    if (user.getEmailVerifiedAt() == null) {
      user.setEmailVerifiedAt(now);
      user = userRepository.save(user);
    }

    Object[] row = userRepository.findUserWithProfileById(user.getId())
        .orElseThrow(() -> new NotFoundException("Profile not found"));
    UserProfilePair pair = unpackUserProfile(row);

    String token = jwtService.generateToken(pair.user().getId(), pair.user().getEmail());
    return new AuthResponse(token, toUserDto(pair.user(), pair.profile()));
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

  private User createOtpUser(String email, String requestedDisplayName) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(randomBootstrapPassword()));
    user.setEmailVerifiedAt(Instant.now());
    user = userRepository.save(user);
    createProfile(user.getId(), defaultDisplayName(email, requestedDisplayName));
    return user;
  }

  private Profile createProfile(Long userId, String displayName) {
    Profile profile = new Profile();
    profile.setUserId(userId);
    profile.setDisplayName(displayName);
    profile.setAvatarUrl(null);
    profile.setBioShort(null);
    profile.setTrekHostEnabled(false);
    profile.setPersonalityTags(Collections.emptyList());
    return profileRepository.save(profile);
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String defaultDisplayName(String email, String requestedDisplayName) {
    if (requestedDisplayName != null && !requestedDisplayName.isBlank()) {
      return requestedDisplayName.trim();
    }
    String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    localPart = localPart.replaceAll("[^A-Za-z0-9]+", " ").trim();
    if (localPart.isBlank()) {
      return "Aurofly User";
    }
    return Character.toUpperCase(localPart.charAt(0)) + localPart.substring(1);
  }

  private String generateOtpCode() {
    int value = secureRandom.nextInt(900000) + 100000;
    return String.valueOf(value);
  }

  private String randomBootstrapPassword() {
    return Long.toHexString(secureRandom.nextLong()) + Long.toHexString(secureRandom.nextLong());
  }

  private String sanitizeAuthAvatar(String avatarUrl) {
    if (avatarUrl == null || avatarUrl.isBlank()) {
      return avatarUrl;
    }
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
