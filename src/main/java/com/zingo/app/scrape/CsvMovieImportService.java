package com.zingo.app.scrape;

import com.zingo.app.entity.City;
import com.zingo.app.entity.Event;
import com.zingo.app.entity.EventType;
import com.zingo.app.entity.ShowFormat;
import com.zingo.app.entity.Showtime;
import com.zingo.app.entity.Venue;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.repository.CityRepository;
import com.zingo.app.repository.EventRepository;
import com.zingo.app.repository.ShowtimeRepository;
import com.zingo.app.repository.VenueRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CsvMovieImportService {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

  private final CityRepository cityRepository;
  private final VenueRepository venueRepository;
  private final EventRepository eventRepository;
  private final ShowtimeRepository showtimeRepository;

  public CsvMovieImportService(CityRepository cityRepository, VenueRepository venueRepository,
      EventRepository eventRepository, ShowtimeRepository showtimeRepository) {
    this.cityRepository = cityRepository;
    this.venueRepository = venueRepository;
    this.eventRepository = eventRepository;
    this.showtimeRepository = showtimeRepository;
  }

  @Transactional
  public CsvMovieImportResult importCsv(MultipartFile file, Long cityId, String postalCode, String cityName) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("CSV file is required");
    }
    City city = resolveCity(cityId, postalCode, cityName);

    int rowsProcessed = 0;
    int rowsSkipped = 0;
    int venuesUpserted = 0;
    int eventsUpserted = 0;
    int showtimesUpserted = 0;

    Map<String, Venue> venueCache = new HashMap<>();
    Map<String, Event> eventCache = new HashMap<>();
    Map<String, Showtime> showtimeCache = new HashMap<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String headerLine = reader.readLine();
      if (headerLine == null) {
        throw new BadRequestException("CSV file is empty");
      }
      List<String> headerCells = parseCsvLine(stripBom(headerLine));
      Map<String, Integer> headerIndex = buildHeaderIndex(headerCells);
      validateHeaders(headerIndex);

      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        try {
          List<String> cells = parseCsvLine(line);
          ParsedRow row = parseRow(cells, headerIndex, city);
          if (row == null) {
            rowsSkipped++;
            continue;
          }
          Venue venue = upsertVenue(city, row, venueCache);
          Event event = upsertEvent(row, eventCache);
          upsertShowtime(venue, event, row, showtimeCache);
          rowsProcessed++;
          venuesUpserted++;
          eventsUpserted++;
          showtimesUpserted++;
        } catch (Exception ex) {
          rowsSkipped++;
        }
      }
    } catch (IOException ex) {
      throw new BadRequestException("Failed to read CSV file");
    }

    return new CsvMovieImportResult(
        city.getName(),
        city.getPostalCode(),
        rowsProcessed,
        rowsSkipped,
        venuesUpserted,
        eventsUpserted,
        showtimesUpserted);
  }

  private City resolveCity(Long cityId, String postalCode, String cityName) {
    if (cityId != null) {
      return cityRepository.findById(cityId).orElseThrow(() -> new BadRequestException("Selected city not found"));
    }
    if (postalCode != null && !postalCode.isBlank()) {
      Optional<City> city = cityRepository.findFirstByPostalCode(postalCode.trim());
      if (city.isPresent()) {
        return city.get();
      }
    }
    if (cityName != null && !cityName.isBlank()) {
      Optional<City> city = cityRepository.findFirstByNameIgnoreCase(cityName.trim());
      if (city.isPresent()) {
        return city.get();
      }
    }
    throw new BadRequestException("Select a city before importing CSV");
  }

  private ParsedRow parseRow(List<String> cells, Map<String, Integer> headerIndex, City city) {
    String movie = value(cells, headerIndex, "movie");
    String theater = value(cells, headerIndex, "theater");
    String date = value(cells, headerIndex, "date");
    String showtime = value(cells, headerIndex, "showtime");
    if (movie == null || theater == null || date == null || showtime == null) {
      return null;
    }
    Instant startsAt = parseStartsAt(date, showtime, city.getTimeZone());
    ShowFormat format = parseFormat(value(cells, headerIndex, "format"));
    String source = normalizeSource(value(cells, headerIndex, "source"));
    return new ParsedRow(movie.trim(), theater.trim(), startsAt, format, source);
  }

  private Venue upsertVenue(City city, ParsedRow row, Map<String, Venue> venueCache) {
    String cacheKey = city.getId() + "|" + row.theaterName().toLowerCase(Locale.ROOT);
    Venue cached = venueCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }
    String venueSourceId = stableId("venue", city.getId() + "|" + row.theaterName());
    Optional<Venue> existing = venueRepository.findBySourceAndSourceId(row.source(), venueSourceId);
    if (existing.isEmpty()) {
      existing = venueRepository.findFirstByCityIdAndNameIgnoreCase(city.getId(), row.theaterName());
    }
    Venue venue = existing.orElseGet(Venue::new);
    venue.setCityId(city.getId());
    venue.setName(row.theaterName());
    venue.setPostalCode(city.getPostalCode());
    venue.setSource(row.source());
    venue.setSourceId(venueSourceId);
    venue = venueRepository.save(venue);
    venueCache.put(cacheKey, venue);
    return venue;
  }

  private Event upsertEvent(ParsedRow row, Map<String, Event> eventCache) {
    String cacheKey = row.movieTitle().toLowerCase(Locale.ROOT);
    Event cached = eventCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }
    String eventSourceId = stableId("event", cacheKey);
    Optional<Event> existing = eventRepository.findBySourceAndSourceId(row.source(), eventSourceId);
    if (existing.isEmpty()) {
      existing = eventRepository.findFirstByTitleIgnoreCaseAndType(row.movieTitle(), EventType.MOVIE);
    }
    Event event = existing.orElseGet(Event::new);
    event.setTitle(row.movieTitle());
    event.setType(EventType.MOVIE);
    event.setSource(row.source());
    event.setSourceId(eventSourceId);
    event = eventRepository.save(event);
    eventCache.put(cacheKey, event);
    return event;
  }

  private Showtime upsertShowtime(Venue venue, Event event, ParsedRow row, Map<String, Showtime> showtimeCache) {
    String cacheKey = event.getId() + "|" + venue.getId() + "|" + row.startsAt() + "|" + row.format();
    Showtime cached = showtimeCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }
    String showtimeSourceId = stableId("showtime", cacheKey);
    Optional<Showtime> existing = showtimeRepository.findBySourceAndSourceId(row.source(), showtimeSourceId);
    if (existing.isEmpty()) {
      existing = showtimeRepository.findFirstByEventIdAndVenueIdAndStartsAtAndFormat(event.getId(), venue.getId(), row.startsAt(), row.format());
    }
    Showtime showtime = existing.orElseGet(Showtime::new);
    showtime.setEventId(event.getId());
    showtime.setVenueId(venue.getId());
    showtime.setStartsAt(row.startsAt());
    showtime.setFormat(row.format());
    showtime.setSource(row.source());
    showtime.setSourceId(showtimeSourceId);
    showtime = showtimeRepository.save(showtime);
    showtimeCache.put(cacheKey, showtime);
    return showtime;
  }

  private Instant parseStartsAt(String dateValue, String timeValue, String timeZone) {
    try {
      LocalDate date = LocalDate.parse(dateValue.trim(), DATE_FORMAT);
      LocalTime time = LocalTime.parse(timeValue.trim().toUpperCase(Locale.ENGLISH), TIME_FORMAT);
      return LocalDateTime.of(date, time)
          .atZone(ZoneId.of(timeZone != null && !timeZone.isBlank() ? timeZone : "Asia/Kolkata"))
          .toInstant();
    } catch (DateTimeParseException ex) {
      throw new BadRequestException("Invalid date/showtime format in CSV");
    }
  }

  private ShowFormat parseFormat(String formatValue) {
    if (formatValue == null) return ShowFormat.GENERAL;
    String normalized = formatValue.trim().toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
    return switch (normalized) {
      case "2D", "TWOD" -> ShowFormat.TWO_D;
      case "3D", "THREED" -> ShowFormat.THREE_D;
      case "IMAX" -> ShowFormat.IMAX;
      default -> ShowFormat.GENERAL;
    };
  }

  private String normalizeSource(String source) {
    if (source == null || source.isBlank()) return "csv-import";
    String normalized = source.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
    return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
  }

  private String stableId(String prefix, String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] bytes = digest.digest((prefix + ":" + value).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private String stripBom(String value) {
    if (value != null && !value.isEmpty() && value.charAt(0) == '\ufeff') {
      return value.substring(1);
    }
    return value;
  }

  private Map<String, Integer> buildHeaderIndex(List<String> headers) {
    Map<String, Integer> index = new HashMap<>();
    for (int i = 0; i < headers.size(); i++) {
      index.put(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
    }
    return index;
  }

  private void validateHeaders(Map<String, Integer> headerIndex) {
    for (String required : List.of("movie", "theater", "date", "showtime")) {
      if (!headerIndex.containsKey(required)) {
        throw new BadRequestException("CSV missing required header: " + required);
      }
    }
  }

  private String value(List<String> cells, Map<String, Integer> headerIndex, String key) {
    Integer index = headerIndex.get(key);
    if (index == null || index < 0 || index >= cells.size()) {
      return null;
    }
    String value = cells.get(index);
    return value == null || value.isBlank() ? null : value;
  }

  private List<String> parseCsvLine(String line) {
    List<String> cells = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (ch == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (ch == ',' && !inQuotes) {
        cells.add(current.toString());
        current.setLength(0);
      } else {
        current.append(ch);
      }
    }
    cells.add(current.toString());
    return cells;
  }

  private record ParsedRow(String movieTitle, String theaterName, Instant startsAt, ShowFormat format, String source) {}
}
