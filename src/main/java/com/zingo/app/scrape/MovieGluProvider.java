package com.zingo.app.scrape;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zingo.app.entity.EventType;
import com.zingo.app.entity.ShowFormat;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MovieGluProvider implements ScrapeProvider {
  private static final Logger log = LoggerFactory.getLogger(MovieGluProvider.class);
  private static final DateTimeFormatter TIME_24H = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendPattern("H:mm")
      .optionalStart()
      .appendPattern(":ss")
      .optionalEnd()
      .toFormatter(Locale.ENGLISH);
  private static final DateTimeFormatter TIME_12H = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendPattern("h:mm")
      .optionalStart()
      .appendPattern(":ss")
      .optionalEnd()
      .optionalStart()
      .appendLiteral(' ')
      .optionalEnd()
      .appendPattern("a")
      .toFormatter(Locale.ENGLISH);

  private final ScrapeConfig config;
  private final ObjectMapper mapper;
  private final HttpClient httpClient;

  public MovieGluProvider(ScrapeConfig config, ObjectMapper mapper) {
    this.config = config;
    this.mapper = mapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  @Override
  public String source() {
    return ScrapeSource.MOVIEGLU.name();
  }

  @Override
  public ScrapeResult scrape(ScrapeRequest request) {
    ScrapeConfig.MovieGlu movieGlu = config.getProviders().getMovieglu();
    if (!movieGlu.isEnabled()) {
      return ScrapeResult.empty();
    }
    MovieGluHeaders headers = resolveHeaders(movieGlu);
    String cityName = ScrapeParsing.resolveCityName(request, config);
    if (isBlank(cityName)) {
      return ScrapeResult.empty();
    }

    ZoneId zone = ZoneId.of(config.getZone());
    if ("sandbox".equals(headers.mode())) {
      return buildStaticSandboxResult(movieGlu, request, cityName, zone);
    }
    if (isBlank(headers.client()) || isBlank(headers.apiKey()) || isBlank(headers.authorization())) {
      log.warn("MovieGlu provider is enabled but {} credentials are missing", headers.mode());
      return ScrapeResult.empty();
    }

    CityInfo city = new CityInfo(cityName, request.postalCode(), zone.getId());
    SyncState syncState = new SyncState();
    List<CinemaRef> cinemas = loadCinemas(movieGlu, headers, syncState, cityName);
    if (cinemas.isEmpty()) {
      return new ScrapeResult(city, List.of(), List.of(), List.of());
    }

    Map<String, ScrapedVenue> venuesById = new LinkedHashMap<>();
    Map<String, ScrapedEvent> eventsById = new LinkedHashMap<>();
    Map<String, ScrapedShowtime> showtimesById = new LinkedHashMap<>();

    int dayCount = Math.max(1, request.days());
    LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now(zone);
    int maxCinemas = movieGlu.getMaxCinemas() > 0 ? movieGlu.getMaxCinemas() : cinemas.size();
    int cinemaCount = Math.min(maxCinemas, cinemas.size());

    for (int cinemaIndex = 0; cinemaIndex < cinemaCount; cinemaIndex++) {
      if (syncState.rateLimited()) {
        break;
      }
      CinemaRef cinema = cinemas.get(cinemaIndex);
      for (int dayOffset = 0; dayOffset < dayCount; dayOffset++) {
        if (syncState.rateLimited()) {
          break;
        }
        LocalDate date = startDate.plusDays(dayOffset);
        JsonNode payload = requestJson(movieGlu, headers, syncState, "cinemaShowTimes/", Map.of(
            "cinema_id", cinema.cinemaId(),
            "date", date.toString()));
        if (payload == null || payload.isMissingNode()) {
          continue;
        }
        collectShowtimes(payload, cinema, date, zone, venuesById, eventsById, showtimesById);
      }
    }

    return new ScrapeResult(city, new ArrayList<>(venuesById.values()), new ArrayList<>(eventsById.values()),
        new ArrayList<>(showtimesById.values()));
  }

  private List<CinemaRef> loadCinemas(ScrapeConfig.MovieGlu movieGlu, MovieGluHeaders headers, SyncState syncState,
      String cityName) {
    int requested = movieGlu.getMaxCinemas() > 0 ? movieGlu.getMaxCinemas() : 10;
    Map<String, String> query = new LinkedHashMap<>();
    query.put("n", String.valueOf(requested));
    query.put("query", cityName);
    JsonNode payload = requestJson(movieGlu, headers, syncState, "cinemaLiveSearch/", query);
    return extractCinemas(payload);
  }

  private List<CinemaRef> extractCinemas(JsonNode payload) {
    if (payload == null || payload.isMissingNode()) {
      return List.of();
    }
    JsonNode cinemasNode = firstArray(payload, "cinemas", "results", "data");
    if (cinemasNode == null && payload.isArray()) {
      cinemasNode = payload;
    }
    if (cinemasNode == null || !cinemasNode.isArray()) {
      return List.of();
    }

    Map<String, CinemaRef> cinemas = new LinkedHashMap<>();
    for (JsonNode cinemaNode : cinemasNode) {
      String cinemaId = firstText(cinemaNode, "cinema_id", "id");
      if (isBlank(cinemaId)) {
        continue;
      }
      String cinemaName = firstText(cinemaNode, "cinema_name", "name");
      if (isBlank(cinemaName)) {
        cinemaName = "Cinema " + cinemaId;
      }
      String address = firstText(cinemaNode, "address", "cinema_address", "full_address");
      String sourceId = normalizeSourceId("movieglu|cinema|" + cinemaId);
      cinemas.putIfAbsent(sourceId, new CinemaRef(cinemaId, cinemaName, address));
    }
    return new ArrayList<>(cinemas.values());
  }

  private List<CinemaRef> sandboxCinemaFallback(int requested) {
    List<CinemaRef> all = List.of(
        new CinemaRef("8842", "Cinema 1", "Big Daddy"),
        new CinemaRef("8845", "Cinema 2", "Deadvlei"),
        new CinemaRef("8910", "Cinema 3", "Dune 45"),
        new CinemaRef("8930", "Cinema 4", "Hiddenvlei"),
        new CinemaRef("9435", "Cinema 5", "Sesriem Canyon"),
        new CinemaRef("10636", "Cinema 6", "Jetty"),
        new CinemaRef("42963", "Cinema 7", "Welwitschia Plains"),
        new CinemaRef("45353", "Cinema 8", "Sandbox Cinema 8"));
    if (requested >= all.size()) {
      return all;
    }
    return all.subList(0, Math.max(requested, 1));
  }

  private List<SandboxFilm> sandboxFilms() {
    return List.of(
        new SandboxFilm("25", "Stargate"),
        new SandboxFilm("1685", "The Adventures of Priscilla, Queen of the Desert"),
        new SandboxFilm("2756", "Max Max"),
        new SandboxFilm("3427", "From Dusk Till Dawn"),
        new SandboxFilm("4167", "Woman in the Dunes"),
        new SandboxFilm("6650", "The English Patient"),
        new SandboxFilm("7772", "Raiders of the Lost Ark"),
        new SandboxFilm("8675", "Lawrence of Arabia - 70mm"),
        new SandboxFilm("21448", "Three Kings"),
        new SandboxFilm("59906", "There will be Blood"),
        new SandboxFilm("62407", "The Fall"),
        new SandboxFilm("184126", "The Martian"));
  }

  private ScrapeResult buildStaticSandboxResult(ScrapeConfig.MovieGlu movieGlu, ScrapeRequest request, String cityName,
      ZoneId zone) {
    CityInfo city = new CityInfo(cityName, request.postalCode(), zone.getId());
    int requested = movieGlu.getMaxCinemas() > 0 ? movieGlu.getMaxCinemas() : 8;
    List<CinemaRef> cinemas = sandboxCinemaFallback(requested);
    List<SandboxFilm> films = sandboxFilms();
    if (cinemas.isEmpty() || films.isEmpty()) {
      return new ScrapeResult(city, List.of(), List.of(), List.of());
    }

    LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now(zone);
    int dayCount = Math.max(1, request.days());

    Map<String, ScrapedVenue> venuesById = new LinkedHashMap<>();
    Map<String, ScrapedEvent> eventsById = new LinkedHashMap<>();
    Map<String, ScrapedShowtime> showtimesById = new LinkedHashMap<>();
    LocalTime[] slots = new LocalTime[] {LocalTime.of(18, 0), LocalTime.of(21, 0)};

    for (int cinemaIndex = 0; cinemaIndex < cinemas.size(); cinemaIndex++) {
      CinemaRef cinema = cinemas.get(cinemaIndex);
      String venueSourceId = normalizeSourceId("movieglu|cinema|" + cinema.cinemaId());
      venuesById.putIfAbsent(venueSourceId,
          new ScrapedVenue(source(), venueSourceId, null, cinema.name(), cinema.address(), null));

      for (int dayOffset = 0; dayOffset < dayCount; dayOffset++) {
        LocalDate date = startDate.plusDays(dayOffset);
        for (int slotIndex = 0; slotIndex < slots.length; slotIndex++) {
          int filmIndex = (cinemaIndex * 3 + dayOffset * 2 + slotIndex) % films.size();
          SandboxFilm film = films.get(filmIndex);
          String eventSourceId = normalizeSourceId("movieglu|film|" + film.filmId());
          eventsById.putIfAbsent(eventSourceId, new ScrapedEvent(source(), eventSourceId, null, film.title(),
              EventType.MOVIE, null));

          Instant startsAt = LocalDateTime.of(date, slots[slotIndex]).atZone(zone).toInstant();
          String showtimeSourceId = normalizeSourceId(String.format(Locale.ENGLISH, "%s|%s|%s|%s|%s",
              source(), eventSourceId, venueSourceId, startsAt, ShowFormat.GENERAL.name()));
          showtimesById.putIfAbsent(showtimeSourceId, new ScrapedShowtime(source(), showtimeSourceId, null,
              eventSourceId, venueSourceId, film.title(), cinema.name(), startsAt, ShowFormat.GENERAL));
        }
      }
    }

    return new ScrapeResult(city, new ArrayList<>(venuesById.values()), new ArrayList<>(eventsById.values()),
        new ArrayList<>(showtimesById.values()));
  }

  private void collectShowtimes(JsonNode payload, CinemaRef fallbackCinema, LocalDate date, ZoneId zone,
      Map<String, ScrapedVenue> venuesById, Map<String, ScrapedEvent> eventsById,
      Map<String, ScrapedShowtime> showtimesById) {
    JsonNode cinemaNode = payload.path("cinema");
    String cinemaId = firstNonBlank(
        firstText(cinemaNode, "cinema_id", "id"),
        fallbackCinema.cinemaId());
    if (isBlank(cinemaId)) {
      return;
    }

    String venueSourceId = normalizeSourceId("movieglu|cinema|" + cinemaId);
    String venueName = firstNonBlank(
        firstText(cinemaNode, "cinema_name", "name"),
        fallbackCinema.name());
    if (!isBlank(venueName) && !isBlank(venueSourceId)) {
      String address = firstNonBlank(
          firstText(cinemaNode, "address", "cinema_address", "full_address"),
          fallbackCinema.address());
      venuesById.putIfAbsent(venueSourceId,
          new ScrapedVenue(source(), venueSourceId, null, venueName, address, null));
    }

    JsonNode films = payload.path("films");
    if (!films.isArray()) {
      return;
    }

    for (JsonNode filmNode : films) {
      String filmId = firstText(filmNode, "film_id", "id");
      String title = firstText(filmNode, "film_name", "name", "title");
      if (isBlank(filmId) && isBlank(title)) {
        continue;
      }

      String eventSourceId = normalizeSourceId("movieglu|film|" + firstNonBlank(filmId, title));
      String posterUrl = extractPosterUrl(filmNode);
      String eventUrl = firstText(filmNode, "film_trailer", "film_url", "url");
      String eventTitle = firstNonBlank(title, "Untitled");
      eventsById.putIfAbsent(eventSourceId, new ScrapedEvent(source(), eventSourceId, eventUrl, eventTitle,
          EventType.MOVIE, posterUrl));

      JsonNode showingsNode = filmNode.get("showings");
      collectShowingNodes(showingsNode, date, zone, eventSourceId, eventTitle, venueSourceId, venueName, showtimesById);
    }
  }

  private void collectShowingNodes(JsonNode node, LocalDate date, ZoneId zone, String eventSourceId, String eventTitle,
      String venueSourceId, String venueName, Map<String, ScrapedShowtime> showtimesById) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        collectShowingNodes(item, date, zone, eventSourceId, eventTitle, venueSourceId, venueName, showtimesById);
      }
      return;
    }
    if (node.isObject()) {
      if (node.has("start_time") || node.has("time") || node.has("show_time") || node.has("showtime")) {
        addShowtime(node, date, zone, null, eventSourceId, eventTitle, venueSourceId, venueName, showtimesById);
      }

      JsonNode timesNode = firstArray(node, "times", "showtimes", "sessions");
      if (timesNode != null) {
        String formatLabel = firstText(node, "format", "showing_format", "showing_dimension", "type", "name");
        collectShowingNodesWithFormat(timesNode, formatLabel, date, zone, eventSourceId, eventTitle, venueSourceId,
            venueName, showtimesById);
      }

      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        if (field.getValue() == timesNode) {
          continue;
        }
        if (field.getValue().isArray() || field.getValue().isObject()) {
          collectShowingNodesWithFormat(field.getValue(), field.getKey(), date, zone, eventSourceId, eventTitle,
              venueSourceId, venueName, showtimesById);
        }
      }
      return;
    }
    if (node.isTextual()) {
      addShowtime(node, date, zone, null, eventSourceId, eventTitle, venueSourceId, venueName, showtimesById);
    }
  }

  private void collectShowingNodesWithFormat(JsonNode node, String formatLabel, LocalDate date, ZoneId zone,
      String eventSourceId, String eventTitle, String venueSourceId, String venueName,
      Map<String, ScrapedShowtime> showtimesById) {
    if (node == null || node.isNull()) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        if (item.isObject()) {
          addShowtime(item, date, zone, formatLabel, eventSourceId, eventTitle, venueSourceId, venueName,
              showtimesById);
          JsonNode nested = firstArray(item, "times", "showtimes", "sessions");
          if (nested != null) {
            String nestedFormat = firstNonBlank(
                firstText(item, "format", "showing_format", "showing_dimension", "type", "name"),
                formatLabel);
            collectShowingNodesWithFormat(nested, nestedFormat, date, zone, eventSourceId, eventTitle, venueSourceId,
                venueName, showtimesById);
          }
        } else {
          addShowtime(item, date, zone, formatLabel, eventSourceId, eventTitle, venueSourceId, venueName,
              showtimesById);
        }
      }
      return;
    }
    if (node.isObject()) {
      addShowtime(node, date, zone, formatLabel, eventSourceId, eventTitle, venueSourceId, venueName, showtimesById);
      JsonNode nested = firstArray(node, "times", "showtimes", "sessions");
      if (nested != null) {
        String nestedFormat = firstNonBlank(
            firstText(node, "format", "showing_format", "showing_dimension", "type", "name"),
            formatLabel);
        collectShowingNodesWithFormat(nested, nestedFormat, date, zone, eventSourceId, eventTitle, venueSourceId,
            venueName, showtimesById);
      }
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        JsonNode value = field.getValue();
        if (value == nested) {
          continue;
        }
        if (value != null && (value.isArray() || value.isObject())) {
          String nextFormat = firstNonBlank(formatLabel, field.getKey());
          collectShowingNodesWithFormat(value, nextFormat, date, zone, eventSourceId, eventTitle, venueSourceId,
              venueName, showtimesById);
        }
      }
    } else if (node.isTextual()) {
      addShowtime(node, date, zone, formatLabel, eventSourceId, eventTitle, venueSourceId, venueName, showtimesById);
    }
  }

  private void addShowtime(JsonNode node, LocalDate date, ZoneId zone, String formatLabel, String eventSourceId,
      String eventTitle, String venueSourceId, String venueName, Map<String, ScrapedShowtime> showtimesById) {
    String start = node.isTextual() ? node.asText()
        : firstText(node, "start_time", "time", "show_time", "showtime");
    if (isBlank(start)) {
      return;
    }
    Instant startsAt = parseShowtime(start, date, zone);
    if (startsAt == null) {
      return;
    }

    String normalizedFormat = firstNonBlank(
        formatLabel,
        firstText(node, "format", "showing_format", "showing_dimension", "type", "name"),
        "General");
    ShowFormat format = resolveFormat(normalizedFormat);
    String sourceUrl = firstText(node, "booking_url", "booking_link", "url");
    String showtimeSourceId = normalizeSourceId(String.format(Locale.ENGLISH, "%s|%s|%s|%s|%s",
        source(), eventSourceId, venueSourceId, startsAt, normalizedFormat));
    if (showtimeSourceId == null) {
      return;
    }
    showtimesById.putIfAbsent(showtimeSourceId, new ScrapedShowtime(source(), showtimeSourceId, sourceUrl,
        eventSourceId, venueSourceId, eventTitle, venueName, startsAt, format));
  }

  private Instant parseShowtime(String value, LocalDate date, ZoneId zone) {
    if (isBlank(value)) {
      return null;
    }
    String trimmed = value.trim();

    Instant instant = ScrapeParsing.parseInstant(trimmed, zone);
    if (instant != null) {
      return instant;
    }

    LocalTime localTime = parseLocalTime(trimmed);
    if (localTime == null) {
      return null;
    }
    return LocalDateTime.of(date, localTime).atZone(zone).toInstant();
  }

  private LocalTime parseLocalTime(String value) {
    try {
      return LocalTime.parse(value, TIME_24H);
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    try {
      return LocalTime.parse(value.toUpperCase(Locale.ENGLISH), TIME_12H);
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    return null;
  }

  private ShowFormat resolveFormat(String value) {
    if (isBlank(value)) {
      return ShowFormat.GENERAL;
    }
    String lower = value.toLowerCase(Locale.ENGLISH);
    if (lower.contains("imax")) {
      return ShowFormat.IMAX;
    }
    if (lower.contains("3d")) {
      return ShowFormat.THREE_D;
    }
    if (lower.contains("2d") || lower.contains("standard")) {
      return ShowFormat.TWO_D;
    }
    return ShowFormat.GENERAL;
  }

  private JsonNode requestJson(ScrapeConfig.MovieGlu movieGlu, MovieGluHeaders headers, SyncState syncState, String path,
      Map<String, String> query) {
    String url = buildUrl(movieGlu.getBaseUrl(), path, query);
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(20))
        .header("Accept", "application/json")
        .header("client", headers.client())
        .header("x-api-key", headers.apiKey())
        .header("authorization", headers.authorization())
        .header("territory", firstNonBlank(headers.territory(), "IN"))
        .header("api-version", firstNonBlank(headers.apiVersion(), "v201"))
        .header("geolocation", firstNonBlank(headers.geolocation(), "20.59;78.96"))
        .header("device-datetime", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
        .GET()
        .build();
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 204) {
        return null;
      }
      if (response.statusCode() == 429) {
        syncState.markRateLimited();
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        String requestId = response.headers().firstValue("x-amzn-requestid").orElse("-");
        String mgMessage = response.headers().firstValue("mg-message").orElse("-");
        String bodySnippet = sanitizeBodySnippet(response.body());
        log.warn("MovieGlu HTTP {} mode={} url={} requestId={} mg-message={} body={}",
            response.statusCode(), headers.mode(), url, requestId, mgMessage, bodySnippet);
        return null;
      }
      String body = response.body();
      if (isBlank(body)) {
        return null;
      }
      return mapper.readTree(body);
    } catch (IOException ex) {
      log.warn("MovieGlu request failed for {}", url, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn("MovieGlu request interrupted for {}", url, ex);
    }
    return null;
  }

  private String buildUrl(String baseUrl, String path, Map<String, String> query) {
    String normalizedBase = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    StringBuilder builder = new StringBuilder(normalizedBase).append(normalizedPath);
    if (query == null || query.isEmpty()) {
      return builder.toString();
    }
    boolean hasQuery = builder.indexOf("?") >= 0;
    for (Map.Entry<String, String> entry : query.entrySet()) {
      if (isBlank(entry.getValue())) {
        continue;
      }
      builder.append(hasQuery ? '&' : '?');
      hasQuery = true;
      builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
          .append('=')
          .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }
    return builder.toString();
  }

  private JsonNode firstArray(JsonNode node, String... fields) {
    if (node == null || fields == null) {
      return null;
    }
    for (String field : fields) {
      JsonNode value = node.get(field);
      if (value != null && value.isArray()) {
        return value;
      }
    }
    return null;
  }

  private String firstText(JsonNode node, String... fields) {
    if (node == null || fields == null) {
      return null;
    }
    for (String field : fields) {
      JsonNode value = node.get(field);
      if (value == null) {
        continue;
      }
      if (value.isTextual()) {
        String text = value.asText();
        if (!isBlank(text)) {
          return text.trim();
        }
      }
      if (value.isNumber()) {
        return value.asText();
      }
    }
    return null;
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    return java.util.Arrays.stream(values)
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .findFirst()
        .orElse(null);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String normalizeSourceId(String value) {
    return ScrapeParsing.normalizeSourceId(value, 120);
  }

  private MovieGluHeaders resolveHeaders(ScrapeConfig.MovieGlu movieGlu) {
    boolean sandbox = movieGlu.isUseSandbox();
    if (sandbox) {
      return new MovieGluHeaders(
          "sandbox",
          firstNonBlank(movieGlu.getSandboxClient(), movieGlu.getClient()),
          firstNonBlank(movieGlu.getSandboxApiKey(), movieGlu.getApiKey()),
          firstNonBlank(movieGlu.getSandboxAuthorization(), movieGlu.getAuthorization()),
          firstNonBlank(movieGlu.getSandboxTerritory(), "XX"),
          firstNonBlank(movieGlu.getSandboxApiVersion(), "v201"),
          firstNonBlank(movieGlu.getSandboxGeolocation(), "52.47;-1.93"));
    }
    return new MovieGluHeaders(
        "live",
        movieGlu.getClient(),
        movieGlu.getApiKey(),
        movieGlu.getAuthorization(),
        firstNonBlank(movieGlu.getTerritory(), "IN"),
        firstNonBlank(movieGlu.getApiVersion(), "v201"),
        firstNonBlank(movieGlu.getGeolocation(), "20.59;78.96"));
  }

  private String extractPosterUrl(JsonNode filmNode) {
    String direct = firstText(filmNode, "film_image", "poster_url", "image_url");
    if (!isBlank(direct)) {
      return direct;
    }
    JsonNode images = filmNode != null ? filmNode.get("images") : null;
    if (images == null || images.isNull()) {
      return null;
    }
    return findFirstTextByField(images, "film_image", 0);
  }

  private String findFirstTextByField(JsonNode node, String fieldName, int depth) {
    if (node == null || node.isNull() || depth > 8) {
      return null;
    }
    if (node.isObject()) {
      JsonNode direct = node.get(fieldName);
      if (direct != null && direct.isTextual() && !direct.asText().isBlank()) {
        return direct.asText().trim();
      }
      Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String found = findFirstTextByField(entry.getValue(), fieldName, depth + 1);
        if (!isBlank(found)) {
          return found;
        }
      }
      return null;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        String found = findFirstTextByField(item, fieldName, depth + 1);
        if (!isBlank(found)) {
          return found;
        }
      }
    }
    return null;
  }

  private String sanitizeBodySnippet(String body) {
    if (body == null || body.isBlank()) {
      return "-";
    }
    String cleaned = body.replaceAll("\\s+", " ").trim();
    if (cleaned.length() <= 240) {
      return cleaned;
    }
    return cleaned.substring(0, 240) + "...";
  }

  private static final class SyncState {
    private boolean rateLimited;

    boolean rateLimited() {
      return rateLimited;
    }

    void markRateLimited() {
      this.rateLimited = true;
    }
  }

  private record MovieGluHeaders(String mode, String client, String apiKey, String authorization, String territory,
      String apiVersion, String geolocation) {}

  private record CinemaRef(String cinemaId, String name, String address) {}

  private record SandboxFilm(String filmId, String title) {}
}
