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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
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

  public Long resolveCityId(String cityName, String postalCode) {
    if (postalCode != null && !postalCode.isBlank()) {
      return cityRepository.findFirstByPostalCode(postalCode).map(City::getId).orElse(null);
    }
    if (cityName != null && !cityName.isBlank()) {
      return cityRepository.findFirstByNameIgnoreCase(cityName).map(City::getId).orElse(null);
    }
    return null;
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
    return eventRepository.findDistinctByCityId(cityId);
  }

  public List<Event> listEventsByType(Long cityId, EventType type) {
    if (type == null) {
      return listEvents(cityId);
    }
    if (cityId == null) {
      return eventRepository.findByType(type);
    }
    return eventRepository.findDistinctByCityIdAndType(cityId, type);
  }

  public List<Event> listMovies(Long cityId) {
    return listEventsByType(cityId, EventType.MOVIE);
  }

  public List<Showtime> listShowtimes(Long eventId, Long venueId, Long cityId) {
    if (eventId != null && venueId != null) {
      return showtimeRepository.findByEventIdAndVenueId(eventId, venueId);
    }
    if (eventId != null) {
      if (cityId == null) {
        return showtimeRepository.findByEventId(eventId);
      }
      return showtimeRepository.findByEventIdAndCityId(eventId, cityId);
    }
    if (venueId != null) {
      return showtimeRepository.findByVenueId(venueId);
    }
    if (cityId != null) {
      return showtimeRepository.findByCityId(cityId);
    }
    return showtimeRepository.findAll();
  }
}
