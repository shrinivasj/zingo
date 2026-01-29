package com.zingo.app.repository;

import com.zingo.app.entity.Event;
import com.zingo.app.entity.EventType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
  List<Event> findByType(EventType type);
}
