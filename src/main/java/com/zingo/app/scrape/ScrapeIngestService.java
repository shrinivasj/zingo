package com.zingo.app.scrape;

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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ScrapeIngestService {
  private final ScrapeOrchestrator orchestrator;
  private final ScrapeConfig config;
  private final PincodeLookupService pincodeLookupService;
  private final CityRepository cityRepository;
  private final VenueRepository venueRepository;
  private final EventRepository eventRepository;
  private final ShowtimeRepository showtimeRepository;

  public ScrapeIngestService(ScrapeOrchestrator orchestrator, ScrapeConfig config,
      PincodeLookupService pincodeLookupService, CityRepository cityRepository, VenueRepository venueRepository,
      EventRepository eventRepository, ShowtimeRepository showtimeRepository) {
    this.orchestrator = orchestrator;
    this.config = config;
    this.pincodeLookupService = pincodeLookupService;
    this.cityRepository = cityRepository;
    this.venueRepository = venueRepository;
    this.eventRepository = eventRepository;
    this.showtimeRepository = showtimeRepository;
  }

  public ScrapeSyncResult sync(String postalCode, String cityName, Integer days) {
    int range = days != null && days > 0 ? days : config.getDays();
    String resolvedCityName = cityName;
    if ((resolvedCityName == null || resolvedCityName.isBlank()) && postalCode != null && !postalCode.isBlank()) {
      resolvedCityName = pincodeLookupService.lookupCity(postalCode);
    }
    ScrapeRequest request = new ScrapeRequest(postalCode, resolvedCityName,
        LocalDate.now(ZoneId.of(config.getZone())), range);
    List<ScrapeResult> results = orchestrator.runAll(request);
    if (results.isEmpty()) {
      return new ScrapeSyncResult(postalCode, resolvedCityName, 0, 0, 0);
    }

    City city = null;
    for (ScrapeResult result : results) {
      CityInfo cityInfo = result.city();
      if (cityInfo != null) {
        city = upsertCity(cityInfo, postalCode, resolvedCityName);
        break;
      }
    }
    if (city == null) {
      city = upsertCity(new CityInfo(resolvedCityName, postalCode, config.getZone()), postalCode, resolvedCityName);
    }

    int venuesUpserted = 0;
    int eventsUpserted = 0;
    int showtimesUpserted = 0;
    Map<String, Venue> venueBySourceId = new HashMap<>();
    Map<String, Event> eventBySourceId = new HashMap<>();

    for (ScrapeResult result : results) {
      for (ScrapedVenue scrapedVenue : result.venues()) {
        if (scrapedVenue.name() == null || scrapedVenue.name().isBlank()) {
          continue;
        }
        Venue venue = upsertVenue(city, scrapedVenue, postalCode);
        venuesUpserted++;
        if (scrapedVenue.sourceId() != null) {
          venueBySourceId.put(scrapedVenue.source() + "|" + scrapedVenue.sourceId(), venue);
        }
      }
      for (ScrapedEvent scrapedEvent : result.events()) {
        if (scrapedEvent.title() == null || scrapedEvent.title().isBlank()) {
          continue;
        }
        Event event = upsertEvent(scrapedEvent);
        eventsUpserted++;
        if (scrapedEvent.sourceId() != null) {
          eventBySourceId.put(scrapedEvent.source() + "|" + scrapedEvent.sourceId(), event);
        }
      }
    }

    for (ScrapeResult result : results) {
      for (ScrapedShowtime scrapedShowtime : result.showtimes()) {
        if (scrapedShowtime.startsAt() == null) {
          continue;
        }
        String eventKey = scrapedShowtime.source() + "|" + scrapedShowtime.eventSourceId();
        Event event = eventBySourceId.get(eventKey);
        if (event == null && scrapedShowtime.eventSourceId() != null) {
          event = eventRepository.findBySourceAndSourceId(scrapedShowtime.source(), scrapedShowtime.eventSourceId())
              .orElse(null);
        }
        if (event == null) {
          continue;
        }
        Venue venue = null;
        String venueKey = scrapedShowtime.source() + "|" + scrapedShowtime.venueSourceId();
        if (scrapedShowtime.venueSourceId() != null) {
          venue = venueBySourceId.get(venueKey);
          if (venue == null) {
            venue = venueRepository.findBySourceAndSourceId(scrapedShowtime.source(), scrapedShowtime.venueSourceId())
                .orElse(null);
          }
        }
        if (venue == null && scrapedShowtime.venueName() != null) {
          venue = venueRepository.findFirstByCityIdAndNameIgnoreCase(city.getId(), scrapedShowtime.venueName())
              .orElse(null);
        }
        if (venue == null) {
          continue;
        }
        upsertShowtime(scrapedShowtime, event, venue);
        showtimesUpserted++;
      }
    }

    return new ScrapeSyncResult(postalCode, city.getName(), venuesUpserted, eventsUpserted, showtimesUpserted);
  }

  private City upsertCity(CityInfo cityInfo, String postalCode, String cityName) {
    Optional<City> existing = Optional.empty();
    if (postalCode != null && !postalCode.isBlank()) {
      existing = cityRepository.findFirstByPostalCode(postalCode);
    }
    if (existing.isEmpty() && cityInfo.name() != null) {
      existing = cityRepository.findFirstByNameIgnoreCase(cityInfo.name());
    }
    if (existing.isEmpty() && cityName != null) {
      existing = cityRepository.findFirstByNameIgnoreCase(cityName);
    }

    City city = existing.orElseGet(City::new);
    String name = cityInfo.name() != null ? cityInfo.name() : cityName;
    if (name != null) {
      city.setName(name);
    }
    if (postalCode != null) {
      city.setPostalCode(postalCode);
    } else if (cityInfo.postalCode() != null) {
      city.setPostalCode(cityInfo.postalCode());
    }
    city.setTimeZone(cityInfo.timeZone() != null ? cityInfo.timeZone() : config.getZone());
    return cityRepository.save(city);
  }

  private Venue upsertVenue(City city, ScrapedVenue scrapedVenue, String fallbackPostalCode) {
    Optional<Venue> existing = Optional.empty();
    if (scrapedVenue.source() != null && scrapedVenue.sourceId() != null) {
      existing = venueRepository.findBySourceAndSourceId(scrapedVenue.source(), scrapedVenue.sourceId());
    }
    if (existing.isEmpty()) {
      existing = venueRepository.findFirstByCityIdAndNameIgnoreCase(city.getId(), scrapedVenue.name());
    }
    Venue venue = existing.orElseGet(Venue::new);
    venue.setCityId(city.getId());
    venue.setName(scrapedVenue.name());
    venue.setAddress(scrapedVenue.address());
    venue.setPostalCode(scrapedVenue.postalCode() != null ? scrapedVenue.postalCode() : fallbackPostalCode);
    venue.setSource(scrapedVenue.source());
    venue.setSourceId(scrapedVenue.sourceId());
    venue.setSourceUrl(scrapedVenue.sourceUrl());
    return venueRepository.save(venue);
  }

  private Event upsertEvent(ScrapedEvent scrapedEvent) {
    Optional<Event> existing = Optional.empty();
    if (scrapedEvent.source() != null && scrapedEvent.sourceId() != null) {
      existing = eventRepository.findBySourceAndSourceId(scrapedEvent.source(), scrapedEvent.sourceId());
    }
    EventType type = scrapedEvent.type() != null ? scrapedEvent.type() : EventType.OTHER;
    if (existing.isEmpty()) {
      existing = eventRepository.findFirstByTitleIgnoreCaseAndType(scrapedEvent.title(), type);
    }
    Event event = existing.orElseGet(Event::new);
    event.setTitle(scrapedEvent.title());
    event.setType(type);
    event.setPosterUrl(scrapedEvent.posterUrl());
    event.setSource(scrapedEvent.source());
    event.setSourceId(scrapedEvent.sourceId());
    event.setSourceUrl(scrapedEvent.sourceUrl());
    return eventRepository.save(event);
  }

  private Showtime upsertShowtime(ScrapedShowtime scrapedShowtime, Event event, Venue venue) {
    Optional<Showtime> existing = Optional.empty();
    if (scrapedShowtime.source() != null && scrapedShowtime.sourceId() != null) {
      existing = showtimeRepository.findBySourceAndSourceId(scrapedShowtime.source(), scrapedShowtime.sourceId());
    }
    ShowFormat format = scrapedShowtime.format() != null ? scrapedShowtime.format() : ShowFormat.GENERAL;
    if (existing.isEmpty()) {
      existing = showtimeRepository.findFirstByEventIdAndVenueIdAndStartsAtAndFormat(event.getId(), venue.getId(),
          scrapedShowtime.startsAt(), format);
    }
    Showtime showtime = existing.orElseGet(Showtime::new);
    showtime.setEventId(event.getId());
    showtime.setVenueId(venue.getId());
    showtime.setStartsAt(scrapedShowtime.startsAt());
    showtime.setFormat(format);
    showtime.setSource(scrapedShowtime.source());
    showtime.setSourceId(scrapedShowtime.sourceId());
    showtime.setSourceUrl(scrapedShowtime.sourceUrl());
    return showtimeRepository.save(showtime);
  }
}
