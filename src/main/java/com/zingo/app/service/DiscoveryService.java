package com.zingo.app.service;

import com.zingo.app.entity.City;
import com.zingo.app.entity.Event;
import com.zingo.app.entity.EventType;
import com.zingo.app.entity.Showtime;
import com.zingo.app.entity.Venue;
import com.zingo.app.repository.CityRepository;
import com.zingo.app.repository.EventRepository;
import com.zingo.app.repository.ShowtimeRepository;
import com.zingo.app.repository.VenueRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DiscoveryService {
  private final CityRepository cityRepository;
  private final VenueRepository venueRepository;
  private final EventRepository eventRepository;
  private final ShowtimeRepository showtimeRepository;

  public DiscoveryService(CityRepository cityRepository, VenueRepository venueRepository, EventRepository eventRepository,
      ShowtimeRepository showtimeRepository) {
    this.cityRepository = cityRepository;
    this.venueRepository = venueRepository;
    this.eventRepository = eventRepository;
    this.showtimeRepository = showtimeRepository;
  }

  public List<City> listCities() {
    return cityRepository.findAll();
  }

  public List<Venue> listVenues(Long cityId) {
    if (cityId == null) {
      return venueRepository.findAll();
    }
    return venueRepository.findByCityId(cityId);
  }

  public List<Event> listEvents(Long cityId) {
    if (cityId == null) {
      return eventRepository.findAll();
    }
    List<Venue> venues = venueRepository.findByCityId(cityId);
    if (venues.isEmpty()) {
      return List.of();
    }
    Set<Long> venueIds = new HashSet<>();
    for (Venue venue : venues) {
      venueIds.add(venue.getId());
    }
    List<Showtime> showtimes = showtimeRepository.findAll();
    Set<Long> eventIds = new HashSet<>();
    for (Showtime showtime : showtimes) {
      if (venueIds.contains(showtime.getVenueId())) {
        eventIds.add(showtime.getEventId());
      }
    }
    if (eventIds.isEmpty()) {
      return List.of();
    }
    List<Event> events = new ArrayList<>();
    for (Long eventId : eventIds) {
      eventRepository.findById(eventId).ifPresent(events::add);
    }
    return events;
  }

  public List<Event> listMovies(Long cityId) {
    if (cityId == null) {
      return eventRepository.findByType(EventType.MOVIE);
    }
    List<Event> events = listEvents(cityId);
    List<Event> filtered = new ArrayList<>();
    for (Event event : events) {
      if (event.getType() == EventType.MOVIE) {
        filtered.add(event);
      }
    }
    return filtered;
  }

  public List<Showtime> listShowtimes(Long eventId, Long venueId) {
    if (eventId != null && venueId != null) {
      return showtimeRepository.findByEventIdAndVenueId(eventId, venueId);
    }
    if (eventId != null) {
      return showtimeRepository.findByEventId(eventId);
    }
    if (venueId != null) {
      return showtimeRepository.findByVenueId(venueId);
    }
    return showtimeRepository.findAll();
  }
}
