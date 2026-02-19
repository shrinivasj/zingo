package com.zingo.app.repository;

import com.zingo.app.entity.LobbyPresence;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LobbyPresenceRepository extends JpaRepository<LobbyPresence, Long> {
  Optional<LobbyPresence> findByShowtimeIdAndUserId(Long showtimeId, Long userId);
  Page<LobbyPresence> findByShowtimeId(Long showtimeId, Pageable pageable);
  long countByShowtimeId(Long showtimeId);
  long deleteByShowtimeIdAndUserId(Long showtimeId, Long userId);
  long deleteByLastSeenAtBefore(Instant cutoff);
}
