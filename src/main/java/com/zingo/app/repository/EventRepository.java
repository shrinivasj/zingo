package com.zingo.app.repository;

import com.zingo.app.entity.Event;
import com.zingo.app.entity.EventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
  List<Event> findByType(EventType type);

  Optional<Event> findFirstByTitleIgnoreCaseAndType(String title, EventType type);

  Optional<Event> findBySourceAndSourceId(String source, String sourceId);
}
