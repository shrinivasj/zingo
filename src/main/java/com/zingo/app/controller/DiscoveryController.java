package com.zingo.app.controller;

import com.zingo.app.dto.DiscoveryDtos.CityDto;
import com.zingo.app.dto.DiscoveryDtos.EventDto;
import com.zingo.app.dto.DiscoveryDtos.ShowtimeDto;
import com.zingo.app.dto.DiscoveryDtos.VenueDto;
import com.zingo.app.entity.City;
import com.zingo.app.entity.Event;
import com.zingo.app.entity.EventType;
import com.zingo.app.entity.Showtime;
import com.zingo.app.entity.Venue;
import com.zingo.app.service.DiscoveryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DiscoveryController {
  private final DiscoveryService discoveryService;

  public DiscoveryController(DiscoveryService discoveryService) {
    this.discoveryService = discoveryService;
  }

  @GetMapping("/cities")
  public List<CityDto> cities() {
    return discoveryService.listCities().stream().map(this::toCityDto).toList();
  }

  @GetMapping("/venues")
  public List<VenueDto> venues(@RequestParam(required = false, name = "cityId") Long cityId,
      @RequestParam(required = false, name = "city") String cityName,
      @RequestParam(required = false, name = "postalCode") String postalCode) {
    Long resolvedCityId = cityId != null ? cityId : discoveryService.resolveCityId(cityName, postalCode);
    return discoveryService.listVenues(resolvedCityId).stream().map(this::toVenueDto).toList();
  }

  @GetMapping("/events")
  public List<EventDto> events(@RequestParam(required = false, name = "cityId") Long cityId,
      @RequestParam(required = false, name = "city") String cityName,
      @RequestParam(required = false, name = "postalCode") String postalCode,
      @RequestParam(required = false, name = "type") String type) {
    Long resolvedCityId = cityId != null ? cityId : discoveryService.resolveCityId(cityName, postalCode);
    EventType eventType = parseEventType(type);
    return discoveryService.listEventsByType(resolvedCityId, eventType).stream().map(this::toEventDto).toList();
  }

  @GetMapping("/showtimes")
  public List<ShowtimeDto> showtimes(@RequestParam(required = false, name = "eventId") Long eventId,
      @RequestParam(required = false, name = "venueId") Long venueId,
      @RequestParam(required = false, name = "city") String cityName,
      @RequestParam(required = false, name = "postalCode") String postalCode) {
    Long resolvedCityId = discoveryService.resolveCityId(cityName, postalCode);
    return discoveryService.listShowtimes(eventId, venueId, resolvedCityId).stream().map(this::toShowtimeDto).toList();
  }

  private CityDto toCityDto(City city) {
    return new CityDto(city.getId(), city.getName());
  }

  private VenueDto toVenueDto(Venue venue) {
    return new VenueDto(venue.getId(), venue.getCityId(), venue.getName());
  }

  private EventDto toEventDto(Event event) {
    return new EventDto(event.getId(), event.getType(), event.getTitle(), event.getPosterUrl());
  }

  private ShowtimeDto toShowtimeDto(Showtime showtime) {
    return new ShowtimeDto(showtime.getId(), showtime.getEventId(), showtime.getVenueId(), showtime.getStartsAt(),
        showtime.getFormat());
  }

  private EventType parseEventType(String type) {
    if (type == null || type.isBlank()) {
      return null;
    }
    try {
      return EventType.valueOf(type.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
