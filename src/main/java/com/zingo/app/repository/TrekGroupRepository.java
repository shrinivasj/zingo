package com.zingo.app.repository;

import com.zingo.app.entity.TrekGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrekGroupRepository extends JpaRepository<TrekGroup, Long> {
  boolean existsByShowtimeId(Long showtimeId);
  List<TrekGroup> findByShowtimeIdOrderByCreatedAtAsc(Long showtimeId);
  List<TrekGroup> findByHostUserIdOrderByCreatedAtDesc(Long hostUserId);
  Optional<TrekGroup> findByShowtimeIdAndHostUserId(Long showtimeId, Long hostUserId);
}
