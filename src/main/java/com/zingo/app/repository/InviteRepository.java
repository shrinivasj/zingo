package com.zingo.app.repository;

import com.zingo.app.entity.Invite;
import com.zingo.app.entity.InviteStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteRepository extends JpaRepository<Invite, Long> {
  long countByFromUserIdAndCreatedAtAfter(Long fromUserId, Instant after);
  Optional<Invite> findTopByFromUserIdOrderByCreatedAtDesc(Long fromUserId);
  Optional<Invite> findByIdAndToUserId(Long id, Long toUserId);
  List<Invite> findByToUserIdAndStatus(Long toUserId, InviteStatus status);
  List<Invite> findByShowtimeId(Long showtimeId);
}
