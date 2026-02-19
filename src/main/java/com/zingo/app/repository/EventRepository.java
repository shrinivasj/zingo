package com.zingo.app.repository;

import com.zingo.app.entity.Event;
import com.zingo.app.entity.EventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {
  List<Event> findByType(EventType type);

  Optional<Event> findFirstByTitleIgnoreCaseAndType(String title, EventType type);

  Optional<Event> findBySourceAndSourceId(String source, String sourceId);

  @Query("select distinct e from Event e join Showtime s on s.eventId = e.id join Venue v on v.id = s.venueId where v.cityId = :cityId")
  List<Event> findDistinctByCityId(@Param("cityId") Long cityId);

  @Query(
      "select distinct e from Event e join Showtime s on s.eventId = e.id join Venue v on v.id = s.venueId "
          + "where v.cityId = :cityId and e.type = :type")
  List<Event> findDistinctByCityIdAndType(@Param("cityId") Long cityId, @Param("type") EventType type);
}
