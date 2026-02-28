package com.zingo.app.repository;

import com.zingo.app.entity.EventType;
import com.zingo.app.entity.ShowFormat;
import com.zingo.app.entity.Showtime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
  List<Showtime> findByEventIdAndVenueId(Long eventId, Long venueId);
  List<Showtime> findByEventId(Long eventId);
  List<Showtime> findByVenueId(Long venueId);
  List<Showtime> findByStartsAtBetween(Instant start, Instant end);
  List<Showtime> findByVenueIdIn(List<Long> venueIds);
  List<Showtime> findByEventIdAndVenueIdIn(Long eventId, List<Long> venueIds);
  long countByEventId(Long eventId);
  long countByVenueId(Long venueId);
  @Query("select s from Showtime s join Venue v on v.id = s.venueId where v.cityId = :cityId")
  List<Showtime> findByCityId(@Param("cityId") Long cityId);
  @Query("select s from Showtime s join Venue v on v.id = s.venueId where s.eventId = :eventId and v.cityId = :cityId")
  List<Showtime> findByEventIdAndCityId(@Param("eventId") Long eventId, @Param("cityId") Long cityId);
  @Query("select s from Showtime s join Event e on e.id = s.eventId where e.type = :type order by s.startsAt desc, s.id desc")
  List<Showtime> findByAdminEventType(@Param("type") EventType type);

  @Query("select count(s) from Showtime s join Event e on e.id = s.eventId where e.type = :type")
  long countByAdminEventType(@Param("type") EventType type);

  Optional<Showtime> findBySourceAndSourceId(String source, String sourceId);

  Optional<Showtime> findFirstByEventIdAndVenueIdAndStartsAtAndFormat(Long eventId, Long venueId, Instant startsAt,
      ShowFormat format);
}
