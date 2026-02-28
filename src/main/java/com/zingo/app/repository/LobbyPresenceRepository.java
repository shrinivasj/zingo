package com.zingo.app.repository;

import com.zingo.app.entity.LobbyPresence;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LobbyPresenceRepository extends JpaRepository<LobbyPresence, Long> {
  Optional<LobbyPresence> findByShowtimeIdAndUserId(Long showtimeId, Long userId);
  java.util.List<LobbyPresence> findByUserId(Long userId);
  Page<LobbyPresence> findByShowtimeId(Long showtimeId, Pageable pageable);
  long countByShowtimeId(Long showtimeId);
  long deleteByShowtimeIdAndUserId(Long showtimeId, Long userId);
  long deleteByShowtimeId(Long showtimeId);
  long deleteByLastSeenAtBefore(Instant cutoff);

  @Modifying
  @Query(value = """
      DELETE lp
      FROM lobby_presence lp
      JOIN showtimes s ON s.id = lp.showtime_id
      JOIN events e ON e.id = s.event_id
      WHERE lp.last_seen_at < :cutoff
        AND e.type <> :trekType
      """, nativeQuery = true)
  int deleteStaleNonTrekPresences(@Param("cutoff") Instant cutoff, @Param("trekType") String trekType);

  @Modifying
  @Query(value = """
      INSERT INTO lobby_presence (showtime_id, user_id, last_seen_at)
      VALUES (:showtimeId, :userId, :lastSeenAt)
      ON DUPLICATE KEY UPDATE last_seen_at = VALUES(last_seen_at)
      """, nativeQuery = true)
  void upsertPresence(@Param("showtimeId") Long showtimeId, @Param("userId") Long userId,
      @Param("lastSeenAt") Instant lastSeenAt);
}
