package com.zingo.app.repository;

import com.zingo.app.entity.ShowFormat;
import com.zingo.app.entity.Showtime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
  List<Showtime> findByEventIdAndVenueId(Long eventId, Long venueId);
  List<Showtime> findByEventId(Long eventId);
  List<Showtime> findByVenueId(Long venueId);
  List<Showtime> findByStartsAtBetween(Instant start, Instant end);
  List<Showtime> findByVenueIdIn(List<Long> venueIds);
  List<Showtime> findByEventIdAndVenueIdIn(Long eventId, List<Long> venueIds);

  Optional<Showtime> findBySourceAndSourceId(String source, String sourceId);

  Optional<Showtime> findFirstByEventIdAndVenueIdAndStartsAtAndFormat(Long eventId, Long venueId, Instant startsAt,
      ShowFormat format);
}
