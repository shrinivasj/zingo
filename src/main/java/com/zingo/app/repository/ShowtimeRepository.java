package com.zingo.app.repository;

import com.zingo.app.entity.Showtime;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
  List<Showtime> findByEventIdAndVenueId(Long eventId, Long venueId);
  List<Showtime> findByEventId(Long eventId);
  List<Showtime> findByVenueId(Long venueId);
  List<Showtime> findByStartsAtBetween(Instant start, Instant end);
}
