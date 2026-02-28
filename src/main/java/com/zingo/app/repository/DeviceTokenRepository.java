package com.zingo.app.repository;

import com.zingo.app.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
  Optional<DeviceToken> findByToken(String token);
  List<DeviceToken> findByUserId(Long userId);
  long deleteByUserIdAndToken(Long userId, String token);
}
