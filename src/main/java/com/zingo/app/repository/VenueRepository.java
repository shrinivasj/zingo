package com.zingo.app.repository;

import com.zingo.app.entity.Venue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {
  List<Venue> findByCityId(Long cityId);
}
