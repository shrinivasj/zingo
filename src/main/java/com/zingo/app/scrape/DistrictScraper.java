package com.zingo.app.scrape;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zingo.app.entity.EventType;
import com.zingo.app.entity.ShowFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class DistrictScraper implements ScrapeProvider {
  private static final Pattern MONTH_PATTERN =
      Pattern.compile("\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern DAY_PATTERN =
      Pattern.compile("\\b(mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern TIME_PATTERN = Pattern.compile("\\b\\d{1,2}:\\d{2}\\s*(am|pm)\\b",
      Pattern.CASE_INSENSITIVE);
  private final ScrapeConfig config;
  private final ScrapeHttpClient httpClient;
  private final ObjectMapper mapper;

  public DistrictScraper(ScrapeConfig config, ScrapeHttpClient httpClient, ObjectMapper mapper) {
    this.config = config;
    this.httpClient = httpClient;
    this.mapper = mapper;
  }

  @Override
  public String source() {
    return ScrapeSource.DISTRICT.name();
  }

  @Override
  public ScrapeResult scrape(ScrapeRequest request) {
    if (!config.getProviders().getDistrict().isEnabled()) {
      return ScrapeResult.empty();
    }
    String cityName = ScrapeParsing.resolveCityName(request, config);
    if (cityName == null) {
      return ScrapeResult.empty();
    }
    String citySlug = ScrapeParsing.slugify(cityName);
    if (citySlug == null) {
      return ScrapeResult.empty();
    }

    ZoneId zone = ZoneId.of(config.getZone());
    String baseUrl = config.getProviders().getDistrict().getBaseUrl();
    String moviesUrl = ScrapeParsing.applyTemplate(baseUrl, config.getProviders().getDistrict().getMoviesPathTemplate(),
        citySlug);
    String listingUrl = ScrapeParsing.applyTemplate(baseUrl, config.getProviders().getDistrict().getEventsPathTemplate(),
        citySlug);

    List<ScrapedEvent> events = new ArrayList<>();
    List<ScrapedVenue> venues = new ArrayList<>();
    List<ScrapedShowtime> showtimes = new ArrayList<>();
    Set<String> seenEventIds = new HashSet<>();

    String html = httpClient.get(listingUrl);
    if (!html.isBlank()) {
      Document document = Jsoup.parse(html, listingUrl);
      extractFromHtmlCards(document, zone, events, venues, showtimes, seenEventIds);
      List<JsonNode> jsonLdNodes = ScrapeParsing.extractJsonLd(document, mapper);
      for (JsonNode node : jsonLdNodes) {
        if (node.has("@graph")) {
          node.get("@graph").forEach(graphNode -> extractEventNode(graphNode, zone, events, venues, showtimes,
              seenEventIds, listingUrl));
        } else {
          extractEventNode(node, zone, events, venues, showtimes, seenEventIds, listingUrl);
        }
      }

      int detailBudget = config.getMaxDetailPages();
      for (Element link : document.select("a[href]")) {
        if (detailBudget <= 0) {
          break;
        }
        String href = link.attr("href");
        if (href == null || href.isBlank()) {
          continue;
        }
        if (!href.contains("/events/")) {
          continue;
        }
        String absolute = link.absUrl("href");
        if (absolute.isBlank() || seenEventIds.contains(absolute)) {
          continue;
        }
        detailBudget--;
        parseDetailPage(absolute, zone, events, venues, showtimes, seenEventIds);
      }
    }

    parseMoviesListing(moviesUrl, cityName, citySlug, zone, events, venues, showtimes, seenEventIds);

    CityInfo city = new CityInfo(cityName, request.postalCode(), zone.getId());
    return new ScrapeResult(city, venues, events, showtimes);
  }

  private void parseDetailPage(String url, ZoneId zone, List<ScrapedEvent> events, List<ScrapedVenue> venues,
      List<ScrapedShowtime> showtimes, Set<String> seenEventIds) {
    String html = httpClient.get(url);
    if (html.isBlank()) {
      return;
    }
    Document document = Jsoup.parse(html, url);
    extractFromHtmlCards(document, zone, events, venues, showtimes, seenEventIds);
    List<JsonNode> jsonLdNodes = ScrapeParsing.extractJsonLd(document, mapper);
    for (JsonNode node : jsonLdNodes) {
      if (node.has("@graph")) {
        node.get("@graph").forEach(graphNode -> extractEventNode(graphNode, zone, events, venues, showtimes,
            seenEventIds, url));
      } else {
        extractEventNode(node, zone, events, venues, showtimes, seenEventIds, url);
      }
    }
  }

  private void extractEventNode(JsonNode node, ZoneId zone, List<ScrapedEvent> events, List<ScrapedVenue> venues,
      List<ScrapedShowtime> showtimes, Set<String> seenEventIds, String fallbackUrl) {
    String type = textValue(node, "@type");
    if (type == null || !type.toLowerCase(Locale.ENGLISH).contains("event")) {
      return;
    }
    String name = textValue(node, "name");
    String url = textValue(node, "url");
    if (url == null) {
      url = fallbackUrl;
    }
    String sourceId = normalizeSourceId(url != null ? url : name);
    if (!seenEventIds.add(sourceId)) {
      return;
    }
    String image = textValue(node, "image");
    EventType eventType = resolveEventType(name, url, node);
    events.add(new ScrapedEvent(source(), sourceId, url, name != null ? name : "Untitled", eventType, image));

    JsonNode location = node.get("location");
    String venueName = location != null ? textValue(location, "name") : null;
    String venueSourceId = venueName != null ? normalizeSourceId(venueName + "|" + sourceId) : null;
    String address = null;
    if (location != null && location.has("address")) {
      JsonNode addressNode = location.get("address");
      address = textValue(addressNode, "streetAddress");
      if (address == null) {
        address = textValue(addressNode, "addressLocality");
      }
    }
    if (venueName != null) {
      venues.add(new ScrapedVenue(source(), venueSourceId, url, venueName, address, null));
    }

    Instant startsAt = ScrapeParsing.parseInstant(textValue(node, "startDate"), zone);
    if (startsAt != null) {
      showtimes.add(new ScrapedShowtime(source(), sourceId + "|start", url, sourceId, venueSourceId, name,
          venueName, startsAt, ShowFormat.GENERAL));
    }
  }

  private void parseMoviesListing(String url, String cityName, String citySlug, ZoneId zone, List<ScrapedEvent> events,
      List<ScrapedVenue> venues, List<ScrapedShowtime> showtimes, Set<String> seenEventIds) {
    String html = httpClient.get(url);
    if (html.isBlank()) {
      return;
    }
    Document document = Jsoup.parse(html, url);
    Elements links = document.select("a[href*=\"/movies/\"]");
    String fallbackVenueName = cityName != null ? "District Movies - " + cityName : "District Movies";
    String fallbackVenueSourceId = normalizeSourceId("district-movies|" + (citySlug != null ? citySlug : "default"));
    boolean fallbackVenueAdded = false;

    for (Element link : links) {
      String href = link.attr("href");
      if (!looksLikeMovieLink(href)) {
        continue;
      }
      String absolute = link.absUrl("href");
      if (absolute.isBlank()) {
        continue;
      }
      String sourceId = normalizeSourceId(absolute);
      if (!seenEventIds.add(sourceId)) {
        continue;
      }
      String title = cleanMovieTitle(link.text());
      if (title == null || title.isBlank()) {
        continue;
      }
      String posterUrl = extractPosterUrl(link);
      events.add(new ScrapedEvent(source(), sourceId, absolute, title, EventType.MOVIE, posterUrl));

      boolean foundShowtimes = parseMovieDetailPage(absolute, sourceId, title, posterUrl, cityName, citySlug, zone,
          events, venues, showtimes);
      if (!foundShowtimes) {
        if (!fallbackVenueAdded) {
          venues.add(new ScrapedVenue(source(), fallbackVenueSourceId, url, fallbackVenueName, null, null));
          fallbackVenueAdded = true;
        }
        Instant startsAt = LocalDate.now(zone).atTime(19, 0).atZone(zone).toInstant();
        showtimes.add(new ScrapedShowtime(source(), sourceId + "|fallback", absolute, sourceId, fallbackVenueSourceId,
            title, fallbackVenueName, startsAt, ShowFormat.GENERAL));
      }
    }
  }

  private boolean parseMovieDetailPage(String url, String eventSourceId, String eventTitle, String posterUrl,
      String cityName, String citySlug, ZoneId zone, List<ScrapedEvent> events, List<ScrapedVenue> venues,
      List<ScrapedShowtime> showtimes) {
    String html = httpClient.get(url);
    if (html.isBlank()) {
      return false;
    }
    Document document = Jsoup.parse(html, url);
    List<JsonNode> jsonLdNodes = ScrapeParsing.extractJsonLd(document, mapper);
    int before = showtimes.size();

    boolean nextDataFound = parseNextDataSessions(document, eventSourceId, eventTitle, zone, cityName, citySlug,
        venues, showtimes);

    for (JsonNode node : jsonLdNodes) {
      collectMovieShowtimes(node, eventSourceId, eventTitle, zone, cityName, citySlug, venues, showtimes);
    }

    String title = eventTitle;
    String image = posterUrl;
    if (!jsonLdNodes.isEmpty()) {
      for (JsonNode node : jsonLdNodes) {
        if (node.has("@graph")) {
          for (JsonNode graphNode : node.get("@graph")) {
            String name = textValue(graphNode, "name");
            if (name != null && !name.isBlank()) {
              title = name;
              break;
            }
          }
        } else {
          String name = textValue(node, "name");
          if (name != null && !name.isBlank()) {
            title = name;
            break;
          }
        }
      }
      if (image == null) {
        for (JsonNode node : jsonLdNodes) {
          if (node.has("@graph")) {
            for (JsonNode graphNode : node.get("@graph")) {
              String img = textValue(graphNode, "image");
              if (img != null && !img.isBlank()) {
                image = img;
                break;
              }
            }
          } else {
            String img = textValue(node, "image");
            if (img != null && !img.isBlank()) {
              image = img;
              break;
            }
          }
        }
      }
    }

    if (title != null && !title.isBlank()) {
      events.add(new ScrapedEvent(source(), eventSourceId, url, title, EventType.MOVIE, image));
    }

    return showtimes.size() > before || nextDataFound;
  }

  private void extractFromHtmlCards(Document document, ZoneId zone, List<ScrapedEvent> events,
      List<ScrapedVenue> venues, List<ScrapedShowtime> showtimes, Set<String> seenEventIds) {
    Elements links = document.select("a[href*=\"/events/\"]");
    for (Element link : links) {
      String href = link.attr("href");
      if (href == null || href.isBlank() || !href.contains("buy-tickets")) {
        continue;
      }
      String absolute = link.absUrl("href");
      if (absolute.isBlank()) {
        continue;
      }
      String sourceId = normalizeSourceId(absolute);
      if (!seenEventIds.add(sourceId)) {
        continue;
      }
      Element titleElement = link.selectFirst("h2, h3, h4, h5");
      if (titleElement == null) {
        continue;
      }
      String title = titleElement.text();
      if (title == null || title.isBlank()) {
        continue;
      }
      Element container = titleElement.parent() != null ? titleElement.parent() : link;
      Elements spans = container.select("span");
      String dateText = null;
      String venueText = null;
      for (Element span : spans) {
        String text = span.text();
        if (text == null || text.isBlank()) {
          continue;
        }
        if (dateText == null && looksLikeDate(text)) {
          dateText = text;
          continue;
        }
        if (venueText == null && !looksLikeDate(text) && !looksLikePrice(text)) {
          venueText = text;
        }
      }

      EventType eventType = resolveEventType(title, absolute, null);
      events.add(new ScrapedEvent(source(), sourceId, absolute, title, eventType, null));

      String venueSourceId = venueText != null ? normalizeSourceId(venueText + "|" + sourceId) : null;
      if (venueText != null) {
        venues.add(new ScrapedVenue(source(), venueSourceId, absolute, venueText, null, null));
      }

      Instant startsAt = parseDistrictDate(dateText, zone);
      if (startsAt != null) {
        showtimes.add(new ScrapedShowtime(source(), sourceId + "|start", absolute, sourceId, venueSourceId,
            title, venueText, startsAt, ShowFormat.GENERAL));
      }
    }
  }

  private String textValue(JsonNode node, String field) {
    if (node == null || !node.has(field)) {
      return null;
    }
    JsonNode value = node.get(field);
    if (value.isTextual()) {
      return value.asText();
    }
    if (value.isNumber() || value.isBoolean()) {
      return value.asText();
    }
    if (value.isArray() && value.size() > 0 && value.get(0).isTextual()) {
      return value.get(0).asText();
    }
    return null;
  }

  private String normalizeSourceId(String raw) {
    return ScrapeParsing.normalizeSourceId(raw, 120);
  }

  private boolean parseNextDataSessions(Document document, String eventSourceId, String eventTitle, ZoneId zone,
      String cityName, String citySlug, List<ScrapedVenue> venues, List<ScrapedShowtime> showtimes) {
    Element script = document.selectFirst("script#__NEXT_DATA__");
    if (script == null) {
      return false;
    }
    String json = script.data();
    if (json == null || json.isBlank()) {
      json = script.html();
    }
    if (json == null || json.isBlank()) {
      return false;
    }
    JsonNode root;
    try {
      root = mapper.readTree(json);
    } catch (Exception ex) {
      return false;
    }

    JsonNode movieSessions = findMovieSessions(root);
    if (movieSessions == null || !movieSessions.isObject()) {
      return false;
    }

    int before = showtimes.size();
    movieSessions.fields().forEachRemaining(entry -> {
      collectCinemaSessions(entry.getValue(), eventSourceId, eventTitle, zone, cityName, citySlug, venues, showtimes);
    });
    return showtimes.size() > before;
  }

  private JsonNode findMovieSessions(JsonNode root) {
    JsonNode node = root.path("props").path("pageProps").path("data").path("serverState").path("movieSessions");
    if (node != null && node.isObject() && node.size() > 0) {
      return node;
    }
    node = root.path("props").path("pageProps").path("initialState").path("movies").path("movieSessions");
    if (node != null && node.isObject() && node.size() > 0) {
      return node;
    }
    return null;
  }

  private void collectCinemaSessions(JsonNode node, String eventSourceId, String eventTitle, ZoneId zone,
      String cityName, String citySlug, List<ScrapedVenue> venues, List<ScrapedShowtime> showtimes) {
    if (node == null) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        collectCinemaSessions(item, eventSourceId, eventTitle, zone, cityName, citySlug, venues, showtimes);
      }
      return;
    }
    if (!node.isObject()) {
      return;
    }

    JsonNode cinemaInfo = node.get("cinemaInfo");
    JsonNode sessions = node.get("sessions");
    if (cinemaInfo != null && sessions != null && sessions.isArray()) {
      String venueName = valueAsText(cinemaInfo, "name");
      if (venueName == null || venueName.isBlank()) {
        venueName = valueAsText(node, "entityName");
      }
      String address = valueAsText(cinemaInfo, "address");
      String pincode = valueAsText(cinemaInfo, "pincode");
      String venueSourceId = venueName != null
          ? normalizeSourceId("district|cinema|" + venueName + "|" + (pincode != null ? pincode : ""))
          : null;

      if (venueName != null && venueSourceId != null) {
        venues.add(new ScrapedVenue(source(), venueSourceId, null, venueName, address, pincode));
      } else {
        String fallbackVenueName = cityName != null ? "District Movies - " + cityName : "District Movies";
        venueSourceId = normalizeSourceId("district-movies|" + (citySlug != null ? citySlug : "default"));
        venues.add(new ScrapedVenue(source(), venueSourceId, null, fallbackVenueName, null, null));
        venueName = fallbackVenueName;
      }

      for (JsonNode session : sessions) {
        String showTime = valueAsText(session, "showTime");
        Instant startsAt = parseDistrictShowTime(showTime);
        if (startsAt == null) {
          continue;
        }
        String sid = valueAsText(session, "sid");
        String showtimeSourceId = sid != null
            ? normalizeSourceId("district|session|" + sid)
            : normalizeSourceId(eventSourceId + "|" + venueSourceId + "|" + showTime);
        ShowFormat format = resolveShowFormat(valueAsText(session, "scrnFmt"));
        showtimes.add(new ScrapedShowtime(source(), showtimeSourceId, null, eventSourceId, venueSourceId, eventTitle,
            venueName, startsAt, format));
      }
    }

    node.fields().forEachRemaining(field -> collectCinemaSessions(field.getValue(), eventSourceId, eventTitle, zone,
        cityName, citySlug, venues, showtimes));
  }

  private String valueAsText(JsonNode node, String field) {
    if (node == null || field == null || !node.has(field)) {
      return null;
    }
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    if (value.isArray() && value.size() > 0) {
      JsonNode first = value.get(0);
      return first != null && first.isValueNode() ? first.asText() : null;
    }
    return value.isValueNode() ? value.asText() : null;
  }

  private ShowFormat resolveShowFormat(String raw) {
    if (raw == null || raw.isBlank()) {
      return ShowFormat.GENERAL;
    }
    String lower = raw.trim().toLowerCase(Locale.ENGLISH);
    if (lower.contains("imax")) {
      return ShowFormat.IMAX;
    }
    if (lower.contains("3d")) {
      return ShowFormat.THREE_D;
    }
    if (lower.contains("2d")) {
      return ShowFormat.TWO_D;
    }
    return ShowFormat.GENERAL;
  }

  private Instant parseDistrictShowTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (Exception ignored) {
      // fall through
    }
    try {
      return java.time.OffsetDateTime.parse(value).toInstant();
    } catch (Exception ignored) {
      // fall through
    }
    try {
      java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(value);
      return localDateTime.atZone(java.time.ZoneOffset.UTC).toInstant();
    } catch (Exception ignored) {
      // fall through
    }
    try {
      java.time.LocalDate date = java.time.LocalDate.parse(value);
      return date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    } catch (Exception ignored) {
      // fall through
    }
    return null;
  }

  private void collectMovieShowtimes(JsonNode node, String eventSourceId, String eventTitle, ZoneId zone,
      String cityName, String citySlug, List<ScrapedVenue> venues, List<ScrapedShowtime> showtimes) {
    if (node == null) {
      return;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        collectMovieShowtimes(item, eventSourceId, eventTitle, zone, cityName, citySlug, venues, showtimes);
      }
      return;
    }
    if (node.has("@graph")) {
      JsonNode graph = node.get("@graph");
      if (graph != null && graph.isArray()) {
        for (JsonNode graphNode : graph) {
          collectMovieShowtimes(graphNode, eventSourceId, eventTitle, zone, cityName, citySlug, venues, showtimes);
        }
      }
      return;
    }
    if (!node.isObject()) {
      return;
    }

    JsonNode itemList = node.get("itemListElement");
    if (itemList != null) {
      if (itemList.isArray()) {
        for (JsonNode item : itemList) {
          JsonNode nested = item.has("item") ? item.get("item") : item;
          collectMovieShowtimes(nested, eventSourceId, eventTitle, zone, cityName, citySlug, venues, showtimes);
        }
      } else {
        collectMovieShowtimes(itemList, eventSourceId, eventTitle, zone, cityName, citySlug, venues, showtimes);
      }
    }

    JsonNode subEvent = node.get("subEvent");
    if (subEvent != null) {
      collectMovieShowtimes(subEvent, eventSourceId, eventTitle, zone, cityName, citySlug, venues, showtimes);
    }
    JsonNode eventSchedule = node.get("eventSchedule");
    if (eventSchedule != null) {
      collectMovieShowtimes(eventSchedule, eventSourceId, eventTitle, zone, cityName, citySlug, venues, showtimes);
    }
    JsonNode hasPart = node.get("hasPart");
    if (hasPart != null) {
      collectMovieShowtimes(hasPart, eventSourceId, eventTitle, zone, cityName, citySlug, venues, showtimes);
    }

    String start = textValue(node, "startDate");
    if (start == null) {
      start = textValue(node, "startTime");
    }
    if (start == null) {
      return;
    }

    Instant startsAt = ScrapeParsing.parseInstant(start, zone);
    if (startsAt == null) {
      return;
    }

    String venueName = extractLocationName(node);
    String venueSourceId;
    if (venueName != null) {
      venueSourceId = normalizeSourceId("venue|" + venueName);
      venues.add(new ScrapedVenue(source(), venueSourceId, null, venueName, null, null));
    } else {
      String fallbackVenueName = cityName != null ? "District Movies - " + cityName : "District Movies";
      venueSourceId = normalizeSourceId("district-movies|" + (citySlug != null ? citySlug : "default"));
      venues.add(new ScrapedVenue(source(), venueSourceId, null, fallbackVenueName, null, null));
      venueName = fallbackVenueName;
    }

    String showtimeSourceId = normalizeSourceId(eventSourceId + "|" + venueSourceId + "|" + startsAt);
    showtimes.add(new ScrapedShowtime(source(), showtimeSourceId, null, eventSourceId, venueSourceId,
        eventTitle, venueName, startsAt, ShowFormat.GENERAL));
  }

  private boolean looksLikeMovieLink(String href) {
    if (href == null || href.isBlank()) {
      return false;
    }
    String lower = href.toLowerCase(Locale.ENGLISH);
    return lower.contains("/movies/") && (lower.contains("movie-tickets") || lower.contains("-mv"));
  }

  private String cleanMovieTitle(String text) {
    if (text == null) {
      return null;
    }
    String trimmed = text.trim();
    if (trimmed.isEmpty()) {
      return trimmed;
    }
    String[] parts = trimmed.split("\\s\\|\\s");
    String first = parts[0].trim();
    return first.replaceAll("\\s(U?A\\d+\\+?|U|A)$", "").trim();
  }

  private String extractPosterUrl(Element link) {
    if (link == null) {
      return null;
    }
    Element img = link.selectFirst("img");
    if (img == null) {
      return null;
    }
    String src = img.absUrl("src");
    return src != null && !src.isBlank() ? src : null;
  }

  private EventType resolveEventType(String name, String url, JsonNode node) {
    if (node != null) {
      String type = textValue(node, "@type");
      if (containsMovieKeyword(type)) {
        return EventType.MOVIE;
      }
      String category = textValue(node, "category");
      if (containsMovieKeyword(category)) {
        return EventType.MOVIE;
      }
      String genre = textValue(node, "genre");
      if (containsMovieKeyword(genre)) {
        return EventType.MOVIE;
      }
      String keywords = textValue(node, "keywords");
      if (containsMovieKeyword(keywords)) {
        return EventType.MOVIE;
      }
      String description = textValue(node, "description");
      if (containsMovieKeyword(description)) {
        return EventType.MOVIE;
      }
    }
    if (containsMovieKeyword(name) || containsMovieKeyword(url)) {
      return EventType.MOVIE;
    }
    return EventType.OTHER;
  }

  private boolean containsMovieKeyword(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    String lower = value.toLowerCase(Locale.ENGLISH);
    return lower.contains("movie")
        || lower.contains("movies")
        || lower.contains("film")
        || lower.contains("cinema")
        || lower.contains("screening");
  }

  private String extractLocationName(JsonNode node) {
    JsonNode location = node.get("location");
    if (location == null) {
      return textValue(node, "locationName");
    }
    if (location.isArray() && location.size() > 0) {
      location = location.get(0);
    }
    if (location != null && location.isObject()) {
      String name = textValue(location, "name");
      if (name != null && !name.isBlank()) {
        return name;
      }
      JsonNode address = location.get("address");
      if (address != null && address.isObject()) {
        String locality = textValue(address, "addressLocality");
        if (locality != null && !locality.isBlank()) {
          return locality;
        }
      }
    }
    return null;
  }

  private boolean looksLikeDate(String text) {
    String trimmed = text.trim();
    return TIME_PATTERN.matcher(trimmed).find()
        || DAY_PATTERN.matcher(trimmed).find()
        || MONTH_PATTERN.matcher(trimmed).find()
        || trimmed.toLowerCase(Locale.ENGLISH).contains("daily")
        || trimmed.toLowerCase(Locale.ENGLISH).contains("multiple slots");
  }

  private boolean looksLikePrice(String text) {
    String lower = text.toLowerCase(Locale.ENGLISH);
    return lower.contains("₹") || lower.contains("rs") || lower.contains("onwards");
  }

  private Instant parseDistrictDate(String text, ZoneId zone) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String lower = text.toLowerCase(Locale.ENGLISH);
    boolean recurring = lower.contains("daily") || lower.contains("multiple slots");
    String cleaned = text.replace("–", "-");
    String[] parts = cleaned.split("\\s-\\s");
    if (parts.length > 0 && !parts[0].isBlank()) {
      cleaned = parts[0].trim();
    }
    cleaned = cleaned.replace("Multiple slots", "").trim();
    if (cleaned.toLowerCase(Locale.ENGLISH).startsWith("daily")) {
      cleaned = cleaned.replaceFirst("(?i)daily,?", "").trim();
    }
    if (cleaned.isBlank()) {
      if (recurring) {
        return LocalDate.now(zone).atTime(19, 0).atZone(zone).toInstant();
      }
      return null;
    }
    if (!TIME_PATTERN.matcher(cleaned).find() && MONTH_PATTERN.matcher(cleaned).find()) {
      cleaned = cleaned + ", 7:00 PM";
    }
    return ScrapeParsing.parseInstant(cleaned, zone);
  }
}
