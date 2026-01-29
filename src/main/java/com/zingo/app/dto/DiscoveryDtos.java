package com.zingo.app.dto;

import com.zingo.app.entity.EventType;
import com.zingo.app.entity.ShowFormat;
import java.time.Instant;

public class DiscoveryDtos {
  public record CityDto(Long id, String name) {}

  public record VenueDto(Long id, Long cityId, String name) {}

  public record EventDto(Long id, EventType type, String title, String posterUrl) {}

  public record ShowtimeDto(Long id, Long eventId, Long venueId, Instant startsAt, ShowFormat format) {}
}
