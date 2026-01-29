package com.zingo.app.config;

import com.zingo.app.entity.City;
import com.zingo.app.entity.Event;
import com.zingo.app.entity.EventType;
import com.zingo.app.entity.ShowFormat;
import com.zingo.app.entity.Showtime;
import com.zingo.app.entity.Venue;
import com.zingo.app.repository.CityRepository;
import com.zingo.app.repository.EventRepository;
import com.zingo.app.repository.ShowtimeRepository;
import com.zingo.app.repository.VenueRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
  private final CityRepository cityRepository;
  private final VenueRepository venueRepository;
  private final EventRepository eventRepository;
  private final ShowtimeRepository showtimeRepository;

  public DataSeeder(CityRepository cityRepository, VenueRepository venueRepository,
      EventRepository eventRepository, ShowtimeRepository showtimeRepository) {
    this.cityRepository = cityRepository;
    this.venueRepository = venueRepository;
    this.eventRepository = eventRepository;
    this.showtimeRepository = showtimeRepository;
  }

  @Override
  public void run(String... args) {
    if (cityRepository.count() > 0) {
      return;
    }

    City city = new City();
    city.setName("Pune");
    city = cityRepository.save(city);

    List<Venue> venues = new ArrayList<>();
    String[] venueNames = {
        "Phoenix Cinemas",
        "Central Mall Screens",
        "Riverfront Multiplex",
        "Old Town Cinema",
        "Hillside IMAX"
    };
    for (String name : venueNames) {
      Venue venue = new Venue();
      venue.setCityId(city.getId());
      venue.setName(name);
      venues.add(venueRepository.save(venue));
    }

    List<Event> events = new ArrayList<>();
    String[] titles = {
        "Starlight Express",
        "The Last Voyager",
        "Monsoon Avenue",
        "City of Echoes",
        "Midnight Library"
    };
    for (String title : titles) {
      Event event = new Event();
      event.setType(EventType.MOVIE);
      event.setTitle(title);
      event.setPosterUrl(null);
      events.add(eventRepository.save(event));
    }

    LocalDate start = LocalDate.now(ZoneOffset.UTC);
    for (int day = 0; day < 7; day++) {
      LocalDate date = start.plusDays(day);
      for (int e = 0; e < events.size(); e++) {
        Event event = events.get(e);
        Venue venue = venues.get((day + e) % venues.size());
        ShowFormat format = ShowFormat.values()[(day + e) % ShowFormat.values().length];
        LocalDateTime showtimeDateTime = LocalDateTime.of(date, LocalTime.of(19, 0));
        Showtime showtime = new Showtime();
        showtime.setEventId(event.getId());
        showtime.setVenueId(venue.getId());
        showtime.setStartsAt(Instant.from(showtimeDateTime.atZone(ZoneOffset.UTC)));
        showtime.setFormat(format);
        showtimeRepository.save(showtime);
      }
    }
  }
}
