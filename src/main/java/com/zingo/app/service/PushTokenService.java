package com.zingo.app.service;

import com.zingo.app.entity.DeviceToken;
import com.zingo.app.repository.DeviceTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushTokenService {
  private final DeviceTokenRepository deviceTokenRepository;

  public PushTokenService(DeviceTokenRepository deviceTokenRepository) {
    this.deviceTokenRepository = deviceTokenRepository;
  }

  @Transactional
  public void registerToken(Long userId, String token, String platform) {
    String normalizedToken = token == null ? "" : token.trim();
    if (normalizedToken.isEmpty()) {
      return;
    }
    String normalizedPlatform = normalizePlatform(platform);
    Instant now = Instant.now();

    DeviceToken entity = deviceTokenRepository.findByToken(normalizedToken).orElseGet(DeviceToken::new);
    entity.setUserId(userId);
    entity.setToken(normalizedToken);
    entity.setPlatform(normalizedPlatform);
    entity.setLastSeenAt(now);
    deviceTokenRepository.save(entity);
  }

  @Transactional
  public void unregisterToken(Long userId, String token) {
    if (token == null || token.isBlank()) {
      return;
    }
    deviceTokenRepository.deleteByUserIdAndToken(userId, token.trim());
  }

  public List<String> listTokensForUser(Long userId) {
    return deviceTokenRepository.findByUserId(userId).stream()
        .map(DeviceToken::getToken)
        .filter(value -> value != null && !value.isBlank())
        .toList();
  }

  private String normalizePlatform(String platform) {
    String value = platform == null ? "" : platform.trim().toLowerCase(Locale.ROOT);
    if (value.isBlank()) {
      return "unknown";
    }
    if (value.length() > 20) {
      return value.substring(0, 20);
    }
    return value;
  }
}
